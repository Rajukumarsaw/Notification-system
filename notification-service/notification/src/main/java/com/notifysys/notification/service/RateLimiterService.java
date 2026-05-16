package com.notifysys.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis-backed rate limiter using sliding window counter.
 * Prevents notification spam per user per time window.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimiterService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${rate-limit.max-notifications-per-user-per-hour:10}")
    private int maxNotificationsPerHour;

    @Value("${rate-limit.max-emails-per-user-per-day:5}")
    private int maxEmailsPerDay;

    private static final String NOTIFICATION_KEY_PREFIX = "rate:notif:";
    private static final String EMAIL_KEY_PREFIX = "rate:email:";

    /**
     * Check if user is allowed to receive a notification.
     * Uses Redis INCR + TTL for atomic sliding window.
     */
    public boolean allowNotification(Long userId) {
        String key = NOTIFICATION_KEY_PREFIX + userId;
        return checkAndIncrement(key, maxNotificationsPerHour, Duration.ofHours(1));
    }

    /**
     * Check if user is allowed to receive an email.
     */
    public boolean allowEmail(Long userId) {
        String key = EMAIL_KEY_PREFIX + userId;
        return checkAndIncrement(key, maxEmailsPerDay, Duration.ofDays(1));
    }

    /**
     * Get remaining quota for a user.
     */
    public int getRemainingNotificationQuota(Long userId) {
        String key = NOTIFICATION_KEY_PREFIX + userId;
        Object count = redisTemplate.opsForValue().get(key);
        if (count == null) return maxNotificationsPerHour;
        return Math.max(0, maxNotificationsPerHour - Integer.parseInt(count.toString()));
    }

    private boolean checkAndIncrement(String key, int limit, Duration window) {
        try {
            Long currentCount = redisTemplate.opsForValue().increment(key);

            if (currentCount == null) {
                log.warn("Redis returned null for key: {}", key);
                return true; // Fail open — don't block if Redis is unavailable
            }

            // Set TTL only on first increment (new window)
            if (currentCount == 1) {
                redisTemplate.expire(key, window);
            }

            boolean allowed = currentCount <= limit;
            if (!allowed) {
                log.warn("Rate limit exceeded for key={}, count={}, limit={}", key, currentCount, limit);
            }
            return allowed;

        } catch (Exception e) {
            log.error("Rate limiter error for key={}: {}", key, e.getMessage());
            return true; // Fail open
        }
    }

    /**
     * Cache recent notifications for fast retrieval (avoid DB hit every time).
     */
    public void cacheNotification(Long userId, String notificationJson) {
        String key = "recent:notif:" + userId;
        // Store as list, keep last 20
        redisTemplate.opsForList().leftPush(key, notificationJson);
        redisTemplate.opsForList().trim(key, 0, 19);
        redisTemplate.expire(key, Duration.ofHours(24));
    }
}
