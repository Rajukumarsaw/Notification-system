package com.notifysys.notification.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    private String id;

    private String eventId;          // From NotificationEvent — idempotency key

    @Indexed
    private Long userId;

    private String recipientEmail;
    private String recipientName;

    private NotificationType type;
    private NotificationChannel channel;
    private NotificationStatus status;

    private String subject;
    private String body;

    private Map<String, Object> metadata;

    private String errorMessage;
    private int retryCount;

    @Indexed
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
    private LocalDateTime updatedAt;

    public enum NotificationType {
        WELCOME_EMAIL,
        PASSWORD_RESET,
        LOGIN_ALERT,
        SYSTEM_ALERT,
        PROMOTIONAL,
        IN_APP
    }

    public enum NotificationChannel {
        EMAIL,
        IN_APP,
        PUSH,
        SMS
    }

    public enum NotificationStatus {
        PENDING,
        PROCESSING,
        SENT,
        FAILED,
        RATE_LIMITED,
        DUPLICATE_SKIPPED
    }
}
