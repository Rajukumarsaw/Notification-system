package com.notifysys.notification.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notifysys.notification.model.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Sends real-time notifications to connected WebSocket clients.
 * Frontend subscribes to /topic/notifications/{userId}
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationWebSocketHandler {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Push a notification to a specific user's WebSocket channel.
     * Frontend client subscribes to /topic/notifications/{userId}
     */
    public void sendToUser(Long userId, String message, Notification notification) {
        String destination = "/topic/notifications/" + userId;
        try {
            WebSocketMessage wsMessage = WebSocketMessage.builder()
                    .type("NOTIFICATION")
                    .message(message)
                    .notificationId(notification.getId())
                    .notificationType(notification.getType() != null ? notification.getType().name() : null)
                    .timestamp(LocalDateTime.now().toString())
                    .build();

            messagingTemplate.convertAndSend(destination, wsMessage);
            log.debug("WebSocket notification sent to userId={}, destination={}", userId, destination);
        } catch (Exception e) {
            log.error("Failed to send WebSocket notification to userId={}: {}", userId, e.getMessage());
        }
    }

    /**
     * Broadcast a system-wide notification to all connected clients.
     */
    public void broadcast(String message) {
        WebSocketMessage wsMessage = WebSocketMessage.builder()
                .type("SYSTEM_BROADCAST")
                .message(message)
                .timestamp(LocalDateTime.now().toString())
                .build();
        messagingTemplate.convertAndSend("/topic/system", wsMessage);
        log.info("System broadcast sent: {}", message);
    }

    @lombok.Builder
    @lombok.Getter
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class WebSocketMessage {
        private String type;
        private String message;
        private String notificationId;
        private String notificationType;
        private String timestamp;
    }
}
