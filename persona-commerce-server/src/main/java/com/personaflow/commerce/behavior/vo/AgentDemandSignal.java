package com.personaflow.commerce.behavior.vo;

import com.personaflow.commerce.behavior.entity.BehaviorEventEntity;
import com.personaflow.commerce.behavior.enums.BehaviorEventType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AgentDemandSignal(
        String eventId,
        String eventType,
        Long skuId,
        Long spuId,
        Long categoryId,
        Long orderId,
        BigDecimal amount,
        boolean preferenceConfirmed,
        boolean fulfilled,
        boolean complementTrigger,
        boolean repeatRecommendationSuppressed,
        LocalDateTime occurredAt
) {

    public static AgentDemandSignal from(BehaviorEventEntity event) {
        boolean paymentSuccess = BehaviorEventType.PAYMENT_SUCCESS.name().equals(event.getEventType());
        return new AgentDemandSignal(
                event.getEventId(),
                event.getEventType(),
                event.getSkuId(),
                event.getSpuId(),
                event.getCategoryId(),
                event.getOrderId(),
                event.getAmount(),
                paymentSuccess,
                paymentSuccess,
                paymentSuccess,
                paymentSuccess,
                event.getOccurredAt()
        );
    }
}
