package com.personaflow.commerce.behavior.dto;

import com.personaflow.commerce.behavior.enums.BehaviorEventType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BehaviorEventCreateCommand(
        String eventId,
        Long userId,
        BehaviorEventType eventType,
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
}
