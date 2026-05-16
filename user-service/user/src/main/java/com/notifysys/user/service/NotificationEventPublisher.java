package com.notifysys.user.service;

import com.notifysys.user.event.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.notification}")
    private String notificationExchange;

    @Value("${rabbitmq.routing-key.welcome-email}")
    private String welcomeEmailRoutingKey;

    @Value("${rabbitmq.routing-key.password-reset}")
    private String passwordResetRoutingKey;

    @Value("${rabbitmq.routing-key.login-alert}")
    private String loginAlertRoutingKey;

    public void publishWelcomeEmail(NotificationEvent event) {
        log.info("Publishing welcome email event for userId={}, eventId={}", event.getUserId(), event.getEventId());
        publish(welcomeEmailRoutingKey, event);
    }

    public void publishPasswordReset(NotificationEvent event) {
        log.info("Publishing password reset event for userId={}", event.getUserId());
        publish(passwordResetRoutingKey, event);
    }

    public void publishLoginAlert(NotificationEvent event) {
        log.info("Publishing login alert event for userId={}", event.getUserId());
        publish(loginAlertRoutingKey, event);
    }

    private void publish(String routingKey, NotificationEvent event) {
        try {
            rabbitTemplate.convertAndSend(notificationExchange, routingKey, event);
            log.debug("Event published successfully: routingKey={}, eventId={}", routingKey, event.getEventId());
        } catch (Exception e) {
            log.error("Failed to publish event: routingKey={}, eventId={}, error={}", 
                      routingKey, event.getEventId(), e.getMessage(), e);
            // In production: persist to outbox table for retry
            throw new RuntimeException("Failed to publish notification event", e);
        }
    }
}
