package com.personaflow.commerce.behavior.messaging;

import com.personaflow.commerce.behavior.dto.BehaviorEventCreateCommand;
import com.personaflow.commerce.behavior.enums.BehaviorEventType;
import com.personaflow.commerce.behavior.service.BehaviorConsumeLogService;
import com.personaflow.commerce.behavior.service.BehaviorEventService;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;

@Component
public class BehaviorEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(BehaviorEventConsumer.class);

    private final BehaviorEventService behaviorEventService;
    private final BehaviorConsumeLogService consumeLogService;

    public BehaviorEventConsumer(
            BehaviorEventService behaviorEventService,
            BehaviorConsumeLogService consumeLogService
    ) {
        this.behaviorEventService = behaviorEventService;
        this.consumeLogService = consumeLogService;
    }

    @RabbitListener(
            queues = BehaviorRabbitConfig.BEHAVIOR_PERSIST_QUEUE,
            containerFactory = "behaviorRabbitListenerContainerFactory"
    )
    public void consume(BehaviorEventMessage message, Message amqpMessage, Channel channel) throws IOException {
        long deliveryTag = amqpMessage.getMessageProperties().getDeliveryTag();
        try {
            validateMessage(message);
            log.info(
                    "Behavior message received messageId={}, eventId={}, eventType={}, userId={}, redelivered={}",
                    message.messageId(),
                    message.eventId(),
                    message.eventType(),
                    message.userId(),
                    amqpMessage.getMessageProperties().isRedelivered()
            );
            if (consumeLogService.isMessageSucceeded(message.messageId())) {
                log.info("Behavior message already succeeded messageId={}, eventId={}", message.messageId(), message.eventId());
                channel.basicAck(deliveryTag, false);
                return;
            }

            BehaviorEventType eventType = parseEventType(message.eventType());
            consumeLogService.markProcessing(message.messageId(), message.eventId());
            behaviorEventService.saveEvent(toCommand(message, eventType));
            consumeLogService.markSuccess(message.messageId(), message.eventId());
            channel.basicAck(deliveryTag, false);
            log.info("Behavior message persisted messageId={}, eventId={}, eventType={}",
                    message.messageId(), message.eventId(), message.eventType());
        } catch (Exception exception) {
            log.warn(
                    "Behavior message failed messageId={}, eventId={}, eventType={}, errorType={}, reason={}",
                    message == null ? null : message.messageId(),
                    message == null ? null : message.eventId(),
                    message == null ? null : message.eventType(),
                    exception.getClass().getSimpleName(),
                    exception.getMessage()
            );
            markFailedSafely(message, exception);
            rethrow(exception);
        }
    }

    private void validateMessage(BehaviorEventMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("Behavior event message must not be null");
        }
        if (!StringUtils.hasText(message.messageId())) {
            throw new IllegalArgumentException("Behavior event messageId must not be blank");
        }
        if (!StringUtils.hasText(message.eventId())) {
            throw new IllegalArgumentException("Behavior event eventId must not be blank");
        }
        if (!StringUtils.hasText(message.eventType())) {
            throw new IllegalArgumentException("Behavior event eventType must not be blank");
        }
    }

    private BehaviorEventType parseEventType(String eventType) {
        try {
            return BehaviorEventType.valueOf(eventType);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Unsupported behavior event type: " + eventType, exception);
        }
    }

    private BehaviorEventCreateCommand toCommand(BehaviorEventMessage message, BehaviorEventType eventType) {
        return new BehaviorEventCreateCommand(
                message.eventId(),
                message.userId(),
                eventType,
                message.sourceModule(),
                message.objectType(),
                message.objectId(),
                message.keyword(),
                message.skuId(),
                message.spuId(),
                message.categoryId(),
                message.orderId(),
                message.amount(),
                message.payloadJson(),
                message.occurredAt()
        );
    }

    private void markFailedSafely(BehaviorEventMessage message, Exception exception) {
        if (message == null || !StringUtils.hasText(message.messageId()) || !StringUtils.hasText(message.eventId())) {
            log.warn("Failed to consume behavior message before consume log could be written", exception);
            return;
        }
        try {
            consumeLogService.markFailed(message.messageId(), message.eventId(), exception.getMessage());
        } catch (RuntimeException logException) {
            log.warn("Failed to update behavior consume log messageId={}, eventId={}",
                    message.messageId(), message.eventId(), logException);
        }
    }

    private void rethrow(Exception exception) throws IOException {
        if (exception instanceof IOException ioException) {
            throw ioException;
        }
        if (exception instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        throw new IllegalStateException("Failed to consume behavior event", exception);
    }
}
