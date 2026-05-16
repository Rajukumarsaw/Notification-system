package com.notifysys.notification.controller;

import com.notifysys.notification.model.Notification;
import com.notifysys.notification.service.NotificationService;
import com.notifysys.notification.service.RateLimiterService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final RateLimiterService rateLimiterService;

    /**
     * GET /api/v1/notifications/history/{userId}
     * Returns paginated notification history from MongoDB
     */
    @GetMapping("/history/{userId}")
    public ResponseEntity<Map<String, Object>> getHistory(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<Notification> history = notificationService.getNotificationHistory(userId, page, size);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", history.getContent(),
                "totalElements", history.getTotalElements(),
                "totalPages", history.getTotalPages(),
                "currentPage", page
        ));
    }

    /**
     * GET /api/v1/notifications/recent/{userId}
     * Returns last 10 notifications (cached in Redis)
     */
    @GetMapping("/recent/{userId}")
    public ResponseEntity<Map<String, Object>> getRecent(@PathVariable Long userId) {
        List<Notification> recent = notificationService.getRecentNotifications(userId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", recent
        ));
    }

    /**
     * GET /api/v1/notifications/quota/{userId}
     * Returns remaining notification quota for rate limiting info
     */
    @GetMapping("/quota/{userId}")
    public ResponseEntity<Map<String, Object>> getQuota(@PathVariable Long userId) {
        int remaining = rateLimiterService.getRemainingNotificationQuota(userId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "userId", userId,
                "remainingQuotaPerHour", remaining
        ));
    }
}
