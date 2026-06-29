package com.personaflow.commerce.behavior.messaging;

import com.personaflow.commerce.behavior.enums.BehaviorEventType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BehaviorEventMessage(
        String messageId,
        String eventId,
        String eventType,
        Long userId,
        String sourceModule,
        String objectType,
        Long objectId,
        String keyword,
        Long skuId,
        Long spuId,
        Long categoryId,
        Long orderId,
        BigDecimal amount,
        String payloadJson,
        LocalDateTime occurredAt,
        String traceId,
        String version
) {

    public static BehaviorEventMessage of(
            String messageId,
            String eventId,
            BehaviorEventType eventType,
            BehaviorEventPayload payload,
            LocalDateTime occurredAt,
            String traceId,
            String version
    ) {
        return new BehaviorEventMessage(
                messageId,
                eventId,
                eventType.name(),
                payload.userId(),
                payload.sourceModule(),
                payload.objectType(),
                payload.objectId(),
                payload.keyword(),
                payload.skuId(),
                payload.spuId(),
                payload.categoryId(),
                payload.orderId(),
                payload.amount(),
                payload.payloadJson(),
                occurredAt,
                traceId,
                version
        );
    }
}
