package com.personaflow.commerce.behavior.messaging;

import com.personaflow.commerce.behavior.enums.BehaviorEventType;

import java.math.BigDecimal;

public record BehaviorEventPublishCommand(
        BehaviorEventType eventType,
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
        Object payload
) {
}
