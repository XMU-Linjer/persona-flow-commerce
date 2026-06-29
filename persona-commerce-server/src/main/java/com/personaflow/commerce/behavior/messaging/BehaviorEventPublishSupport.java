package com.personaflow.commerce.behavior.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class BehaviorEventPublishSupport {

    private static final Logger log = LoggerFactory.getLogger(BehaviorEventPublishSupport.class);
    private static final String MESSAGE_VERSION = "1.0";

    private final BehaviorEventPublisher behaviorEventPublisher;
    private final ObjectMapper objectMapper;

    public BehaviorEventPublishSupport(
            BehaviorEventPublisher behaviorEventPublisher,
            ObjectMapper objectMapper
    ) {
        this.behaviorEventPublisher = behaviorEventPublisher;
        this.objectMapper = objectMapper;
    }

    public void publish(BehaviorEventPublishCommand command) {
        publishSafely(command);
    }

    public void publishAfterCommit(BehaviorEventPublishCommand command) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publishSafely(command);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publishSafely(command);
            }
        });
    }

    private void publishSafely(BehaviorEventPublishCommand command) {
        try {
            behaviorEventPublisher.publish(toMessage(command));
        } catch (RuntimeException exception) {
            log.warn(
                    "Failed to publish behavior event eventType={}, userId={}, sourceModule={}, objectId={}, reason={}",
                    command == null ? null : command.eventType(),
                    command == null ? null : command.userId(),
                    command == null ? null : command.sourceModule(),
                    command == null ? null : command.objectId(),
                    exception.getMessage()
            );
        }
    }

    private BehaviorEventMessage toMessage(BehaviorEventPublishCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Behavior event publish command must not be null");
        }
        return new BehaviorEventMessage(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                command.eventType().name(),
                command.userId(),
                command.sourceModule(),
                command.objectType(),
                command.objectId(),
                command.keyword(),
                command.skuId(),
                command.spuId(),
                command.categoryId(),
                command.orderId(),
                command.amount(),
                toPayloadJson(command.payload()),
                LocalDateTime.now(),
                UUID.randomUUID().toString(),
                MESSAGE_VERSION
        );
    }

    private String toPayloadJson(Object payload) {
        if (payload == null) {
            return null;
        }
        if (payload instanceof String payloadJson) {
            return payloadJson;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize behavior event payload", exception);
        }
    }
}
