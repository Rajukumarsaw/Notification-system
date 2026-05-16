package com.notifysys.notification.event;

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
    private String eventType;
    private String recipientEmail;
    private String recipientName;
    private Long userId;
    private Map<String, Object> payload;
    private LocalDateTime createdAt;
    private int priority;
}
