package com.notifysys.notification.repository;

import com.notifysys.notification.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {

    // Idempotency check — don't process the same event twice
    Optional<Notification> findByEventId(String eventId);

    // Paginated notification history for a user
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // Recent notifications for real-time display
    List<Notification> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);

    // Count notifications for rate limiting analytics
    long countByUserIdAndChannelAndCreatedAtAfter(
            Long userId,
            Notification.NotificationChannel channel,
            LocalDateTime after
    );

    // Failed notifications for retry jobs
    List<Notification> findByStatusAndRetryCountLessThan(
            Notification.NotificationStatus status,
            int maxRetries
    );
}
