package com.notifysys.user.controller;

import com.notifysys.user.dto.UserDto;
import com.notifysys.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    /**
     * POST /api/v1/users/register
     * Registers a new user and triggers async welcome email via RabbitMQ
     */
    @PostMapping("/register")
    public ResponseEntity<UserDto.ApiResponse<UserDto.AuthResponse>> register(
            @Valid @RequestBody UserDto.RegisterRequest request) {
        log.info("Registration request for email: {}", request.getEmail());
        UserDto.AuthResponse response = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(UserDto.ApiResponse.success("User registered successfully", response));
    }

    /**
     * POST /api/v1/users/login
     * Authenticates user and publishes login alert event
     */
    @PostMapping("/login")
    public ResponseEntity<UserDto.ApiResponse<UserDto.AuthResponse>> login(
            @Valid @RequestBody UserDto.LoginRequest request,
            HttpServletRequest httpRequest) {
        String clientIp = getClientIp(httpRequest);
        UserDto.AuthResponse response = userService.login(request, clientIp);
        return ResponseEntity.ok(UserDto.ApiResponse.success("Login successful", response));
    }

    /**
     * POST /api/v1/users/password-reset
     * Sends password reset email asynchronously
     */
    @PostMapping("/password-reset")
    public ResponseEntity<UserDto.ApiResponse<Void>> requestPasswordReset(@RequestParam String email) {
        userService.requestPasswordReset(email);
        return ResponseEntity.ok(UserDto.ApiResponse.success(
                "If an account exists with this email, a reset link has been sent.", null));
    }

    /**
     * GET /api/v1/users/{userId}
     * Returns cached user profile
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserDto.ApiResponse<UserDto.UserResponse>> getUserById(@PathVariable Long userId) {
        UserDto.UserResponse user = userService.getUserById(userId);
        return ResponseEntity.ok(UserDto.ApiResponse.success("User retrieved", user));
    }

    /**
     * PATCH /api/v1/users/{userId}/notifications
     * Toggle notification preferences
     */
    @PatchMapping("/{userId}/notifications")
    public ResponseEntity<UserDto.ApiResponse<UserDto.UserResponse>> updateNotificationPreference(
            @PathVariable Long userId,
            @RequestParam boolean enabled) {
        UserDto.UserResponse user = userService.updateNotificationPreference(userId, enabled);
        return ResponseEntity.ok(UserDto.ApiResponse.success("Notification preference updated", user));
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
