package com.notifysys.user.event;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEvent implements Serializable {

    private String eventId;
    private String eventType;       // WELCOME_EMAIL, LOGIN_ALERT, PASSWORD_RESET
    private String recipientEmail;
    private String recipientName;
    private Long userId;
    private Map<String, Object> payload;  // Dynamic data for email templates

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private int priority;           // 1 = high, 5 = low

    // Factory methods for common events
    public static NotificationEvent welcomeEmail(Long userId, String email, String name) {
        return NotificationEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .eventType("WELCOME_EMAIL")
                .recipientEmail(email)
                .recipientName(name)
                .userId(userId)
                .priority(1)
                .payload(Map.of(
                        "username", name,
                        "loginUrl", "https://app.notifysys.com/login"
                ))
                .build();
    }

    public static NotificationEvent loginAlert(Long userId, String email, String name, String ipAddress) {
        return NotificationEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .eventType("LOGIN_ALERT")
                .recipientEmail(email)
                .recipientName(name)
                .userId(userId)
                .priority(2)
                .payload(Map.of(
                        "ipAddress", ipAddress,
                        "loginTime", LocalDateTime.now().toString()
                ))
                .build();
    }

    public static NotificationEvent passwordReset(Long userId, String email, String name, String resetToken) {
        return NotificationEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .eventType("PASSWORD_RESET")
                .recipientEmail(email)
                .recipientName(name)
                .userId(userId)
                .priority(1)
                .payload(Map.of(
                        "resetToken", resetToken,
                        "resetUrl", "https://app.notifysys.com/reset-password?token=" + resetToken,
                        "expiryMinutes", 15
                ))
                .build();
    }
}
