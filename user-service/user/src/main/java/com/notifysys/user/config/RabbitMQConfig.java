package com.notifysys.user.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange.notification}")
    private String notificationExchange;

    @Value("${rabbitmq.routing-key.welcome-email}")
    private String welcomeEmailRoutingKey;

    @Value("${rabbitmq.routing-key.password-reset}")
    private String passwordResetRoutingKey;

    @Value("${rabbitmq.routing-key.login-alert}")
    private String loginAlertRoutingKey;

    // Exchange — all notification events pass through this
    @Bean
    public TopicExchange notificationExchange() {
        return ExchangeBuilder.topicExchange(notificationExchange)
                .durable(true)
                .build();
    }

    // Queues
    @Bean
    public Queue welcomeEmailQueue() {
        return QueueBuilder.durable("q.welcome.email")
                .withArgument("x-dead-letter-exchange", "dlx.notification")
                .withArgument("x-dead-letter-routing-key", "dlq.welcome.email")
                .build();
    }

    @Bean
    public Queue passwordResetQueue() {
        return QueueBuilder.durable("q.password.reset")
                .withArgument("x-dead-letter-exchange", "dlx.notification")
                .withArgument("x-dead-letter-routing-key", "dlq.password.reset")
                .build();
    }

    @Bean
    public Queue loginAlertQueue() {
        return QueueBuilder.durable("q.login.alert")
                .withArgument("x-dead-letter-exchange", "dlx.notification")
                .withArgument("x-dead-letter-routing-key", "dlq.login.alert")
                .build();
    }

    // Dead Letter Exchange for failed messages
    @Bean
    public DirectExchange deadLetterExchange() {
        return ExchangeBuilder.directExchange("dlx.notification").durable(true).build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable("q.dead.letter").build();
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with("dlq.#");
    }

    // Bindings — connect queues to exchange with routing keys
    @Bean
    public Binding welcomeEmailBinding() {
        return BindingBuilder.bind(welcomeEmailQueue())
                .to(notificationExchange())
                .with(welcomeEmailRoutingKey);
    }

    @Bean
    public Binding passwordResetBinding() {
        return BindingBuilder.bind(passwordResetQueue())
                .to(notificationExchange())
                .with(passwordResetRoutingKey);
    }

    @Bean
    public Binding loginAlertBinding() {
        return BindingBuilder.bind(loginAlertQueue())
                .to(notificationExchange())
                .with(loginAlertRoutingKey);
    }

    // JSON serialization for messages
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        // Enable publisher confirms for reliability
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                System.err.println("Message not acknowledged: " + cause);
            }
        });
        return template;
    }
}
