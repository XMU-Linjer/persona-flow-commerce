package com.personaflow.commerce.behavior.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class RabbitBehaviorEventPublisher implements BehaviorEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitBehaviorEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final BehaviorRoutingKeyResolver routingKeyResolver;

    public RabbitBehaviorEventPublisher(
            RabbitTemplate rabbitTemplate,
            BehaviorRoutingKeyResolver routingKeyResolver
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.routingKeyResolver = routingKeyResolver;
    }

    @Override
    public void publish(BehaviorEventMessage message) {
        String routingKey = routingKeyResolver.resolve(message.eventType());
        try {
            rabbitTemplate.convertAndSend(
                    BehaviorRabbitConfig.BEHAVIOR_EXCHANGE,
                    routingKey,
                    message,
                    amqpMessage -> {
                        amqpMessage.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                        amqpMessage.getMessageProperties().setMessageId(message.messageId());
                        return amqpMessage;
                    }
            );
        } catch (RuntimeException exception) {
            log.warn("Failed to publish behavior event messageId={}, eventId={}, reason={}",
                    message.messageId(), message.eventId(), exception.getMessage());
            throw new IllegalStateException("Failed to publish behavior event", exception);
        }
    }
}
