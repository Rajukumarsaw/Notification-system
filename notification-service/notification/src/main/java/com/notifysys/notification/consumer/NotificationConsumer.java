package com.notifysys.notification.consumer;

import com.notifysys.notification.event.NotificationEvent;
import com.notifysys.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Listens to RabbitMQ queues and delegates to NotificationService.
 * Each queue has its own listener for independent scaling.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final NotificationService notificationService;

    /**
     * Consumes welcome email events from q.welcome.email queue.
     * Decoupled from user registration — runs asynchronously.
     */
    @RabbitListener(queues = "q.welcome.email", concurrency = "2-5")
    public void consumeWelcomeEmail(@Payload NotificationEvent event) {
        log.info("Received welcome email event: eventId={}, recipient={}",
                event.getEventId(), event.getRecipientEmail());
        notificationService.processEvent(event);
    }

    /**
     * Consumes password reset events from q.password.reset queue.
     * High priority — processes immediately.
     */
    @RabbitListener(queues = "q.password.reset", concurrency = "1-3")
    public void consumePasswordReset(@Payload NotificationEvent event) {
        log.info("Received password reset event: eventId={}", event.getEventId());
        notificationService.processEvent(event);
    }

    /**
     * Consumes login alert events from q.login.alert queue.
     */
    @RabbitListener(queues = "q.login.alert", concurrency = "2-4")
    public void consumeLoginAlert(@Payload NotificationEvent event) {
        log.info("Received login alert event: eventId={}, userId={}", 
                event.getEventId(), event.getUserId());
        notificationService.processEvent(event);
    }

    /**
     * Dead letter queue consumer — handles failed messages after all retries.
     * In production: alert on-call team, store for manual review.
     */
    @RabbitListener(queues = "q.dead.letter")
    public void consumeDeadLetter(@Payload Object message) {
        log.error("Dead letter message received — all retries exhausted: {}", message);
        // TODO: Alert monitoring system (PagerDuty, etc.)
    }
}
