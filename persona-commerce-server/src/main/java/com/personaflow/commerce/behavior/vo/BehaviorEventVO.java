package com.personaflow.commerce.behavior.vo;

import com.personaflow.commerce.behavior.entity.BehaviorEventEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BehaviorEventVO(
        Long id,
        String eventId,
        String eventType,
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
        LocalDateTime occurredAt
) {

    public static BehaviorEventVO from(BehaviorEventEntity event) {
        return new BehaviorEventVO(
                event.getId(),
                event.getEventId(),
                event.getEventType(),
                event.getSourceModule(),
                event.getObjectType(),
                event.getObjectId(),
                event.getKeyword(),
                event.getSkuId(),
                event.getSpuId(),
                event.getCategoryId(),
                event.getOrderId(),
                event.getAmount(),
                event.getPayloadJson(),
                event.getOccurredAt()
        );
    }
}
