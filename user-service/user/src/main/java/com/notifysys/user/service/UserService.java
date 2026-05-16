package com.notifysys.user.service;

import com.notifysys.user.dto.UserDto;
import com.notifysys.user.event.NotificationEvent;
import com.notifysys.user.model.User;
import com.notifysys.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final NotificationEventPublisher eventPublisher;

    @Transactional
    public UserDto.AuthResponse register(UserDto.RegisterRequest request) {
        // Validate uniqueness
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already taken: " + request.getUsername());
        }

        // Persist user
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .build();

        user = userRepository.save(user);
        log.info("New user registered: userId={}, email={}", user.getId(), user.getEmail());

        // Publish async welcome email event to RabbitMQ — decoupled from HTTP response
        try {
            NotificationEvent event = NotificationEvent.welcomeEmail(
                    user.getId(), user.getEmail(), user.getFirstName()
            );
            eventPublisher.publishWelcomeEmail(event);
        } catch (Exception e) {
            // Don't fail registration if notification fails — eventual consistency
            log.warn("Failed to publish welcome email event for userId={}: {}", user.getId(), e.getMessage());
        }

        // Generate JWT
        String token = jwtService.generateToken(user.getUsername(), user.getId(), user.getEmail());

        return UserDto.AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationMs())
                .user(UserDto.toResponse(user))
                .build();
    }

    @Transactional(readOnly = true)
    public UserDto.AuthResponse login(UserDto.LoginRequest request, String clientIp) {
        User user = userRepository.findByUsernameOrEmail(request.getUsernameOrEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Failed login attempt for: {}", request.getUsernameOrEmail());
            throw new BadCredentialsException("Invalid credentials");
        }

        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw new IllegalStateException("Account is " + user.getStatus().name().toLowerCase());
        }

        log.info("User logged in: userId={}", user.getId());

        // Publish async login alert event
        try {
            NotificationEvent event = NotificationEvent.loginAlert(
                    user.getId(), user.getEmail(), user.getFirstName(), clientIp
            );
            eventPublisher.publishLoginAlert(event);
        } catch (Exception e) {
            log.warn("Failed to publish login alert for userId={}: {}", user.getId(), e.getMessage());
        }

        String token = jwtService.generateToken(user.getUsername(), user.getId(), user.getEmail());

        return UserDto.AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationMs())
                .user(UserDto.toResponse(user))
                .build();
    }

    @Transactional
    public void requestPasswordReset(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String resetToken = UUID.randomUUID().toString();
            // In production: store token hash in DB with expiry

            try {
                NotificationEvent event = NotificationEvent.passwordReset(
                        user.getId(), user.getEmail(), user.getFirstName(), resetToken
                );
                eventPublisher.publishPasswordReset(event);
                log.info("Password reset event published for userId={}", user.getId());
            } catch (Exception e) {
                log.error("Failed to publish password reset event for userId={}: {}", user.getId(), e.getMessage());
            }
        });
        // Always return success to prevent email enumeration attacks
    }

    @Cacheable(value = "users", key = "#userId")
    public UserDto.UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        return UserDto.toResponse(user);
    }

    @CacheEvict(value = "users", key = "#userId")
    @Transactional
    public UserDto.UserResponse updateNotificationPreference(Long userId, boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        user.setNotificationEnabled(enabled);
        return UserDto.toResponse(userRepository.save(user));
    }
}
