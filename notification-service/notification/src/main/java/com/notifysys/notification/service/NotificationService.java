package com.notifysys.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notifysys.notification.event.NotificationEvent;
import com.notifysys.notification.model.Notification;
import com.notifysys.notification.repository.NotificationRepository;
import com.notifysys.notification.websocket.NotificationWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final RateLimiterService rateLimiterService;
    private final NotificationWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;

    /**
     * Main entry point called by RabbitMQ consumers.
     * Handles idempotency, rate limiting, and routing to correct channel.
     */
    public void processEvent(NotificationEvent event) {
        log.info("Processing notification event: eventId={}, type={}", event.getEventId(), event.getEventType());

        // 1. Idempotency check — skip if already processed
        if (notificationRepository.findByEventId(event.getEventId()).isPresent()) {
            log.warn("Duplicate event skipped: eventId={}", event.getEventId());
            return;
        }

        // 2. Rate limit check
        if (!rateLimiterService.allowNotification(event.getUserId())) {
            log.warn("Rate limit exceeded for userId={}", event.getUserId());
            saveNotification(event, Notification.NotificationStatus.RATE_LIMITED, null, null);
            return;
        }

        // 3. Route to correct handler
        try {
            switch (event.getEventType()) {
                case "WELCOME_EMAIL" -> processWelcomeEmail(event);
                case "PASSWORD_RESET" -> processPasswordReset(event);
                case "LOGIN_ALERT" -> processLoginAlert(event);
                default -> log.warn("Unknown event type: {}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("Failed to process event: eventId={}, error={}", event.getEventId(), e.getMessage(), e);
            saveNotification(event, Notification.NotificationStatus.FAILED, null, e.getMessage());
            throw e; // Re-throw so RabbitMQ can retry
        }
    }

    private void processWelcomeEmail(NotificationEvent event) {
        if (!rateLimiterService.allowEmail(event.getUserId())) {
            saveNotification(event, Notification.NotificationStatus.RATE_LIMITED,
                    Notification.NotificationType.WELCOME_EMAIL, "Email rate limit exceeded");
            return;
        }

        emailService.sendWelcomeEmail(event.getRecipientEmail(), event.getRecipientName(), event.getPayload());

        Notification saved = saveNotification(event, Notification.NotificationStatus.SENT,
                Notification.NotificationType.WELCOME_EMAIL, null);

        // Push real-time in-app notification via WebSocket
        sendWebSocketNotification(event.getUserId(), "Welcome to NotifySys!", saved);
    }

    private void processPasswordReset(NotificationEvent event) {
        emailService.sendPasswordResetEmail(event.getRecipientEmail(), event.getRecipientName(), event.getPayload());
        saveNotification(event, Notification.NotificationStatus.SENT,
                Notification.NotificationType.PASSWORD_RESET, null);
    }

    private void processLoginAlert(NotificationEvent event) {
        emailService.sendLoginAlertEmail(event.getRecipientEmail(), event.getRecipientName(), event.getPayload());

        Notification saved = saveNotification(event, Notification.NotificationStatus.SENT,
                Notification.NotificationType.LOGIN_ALERT, null);

        // Also push real-time alert via WebSocket
        sendWebSocketNotification(event.getUserId(), "New login detected on your account", saved);
    }

    private Notification saveNotification(NotificationEvent event, Notification.NotificationStatus status,
                                           Notification.NotificationType type, String errorMessage) {
        Notification notification = Notification.builder()
                .eventId(event.getEventId())
                .userId(event.getUserId())
                .recipientEmail(event.getRecipientEmail())
                .recipientName(event.getRecipientName())
                .type(type != null ? type : Notification.NotificationType.IN_APP)
                .channel(Notification.NotificationChannel.EMAIL)
                .status(status)
                .errorMessage(errorMessage)
                .createdAt(LocalDateTime.now())
                .sentAt(status == Notification.NotificationStatus.SENT ? LocalDateTime.now() : null)
                .metadata(event.getPayload())
                .build();

        Notification saved = notificationRepository.save(notification);

        // Cache in Redis for fast recent-notification retrieval
        try {
            String json = objectMapper.writeValueAsString(saved);
            rateLimiterService.cacheNotification(event.getUserId(), json);
        } catch (Exception e) {
            log.warn("Failed to cache notification in Redis: {}", e.getMessage());
        }

        return saved;
    }

    private void sendWebSocketNotification(Long userId, String message, Notification notification) {
        try {
            webSocketHandler.sendToUser(userId, message, notification);
        } catch (Exception e) {
            log.warn("WebSocket notification failed for userId={}: {}", userId, e.getMessage());
        }
    }

    // REST endpoints — query notification history from MongoDB
    public Page<Notification> getNotificationHistory(Long userId, int page, int size) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(page, size));
    }

    public List<Notification> getRecentNotifications(Long userId) {
        return notificationRepository.findTop10ByUserIdOrderByCreatedAtDesc(userId);
    }
}
