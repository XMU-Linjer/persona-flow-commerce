package com.personaflow.commerce.behavior.messaging;

import java.math.BigDecimal;

public record BehaviorEventPayload(
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
        String payloadJson
) {
}
