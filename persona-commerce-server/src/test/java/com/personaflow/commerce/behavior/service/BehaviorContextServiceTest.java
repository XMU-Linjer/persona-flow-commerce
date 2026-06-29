package com.personaflow.commerce.behavior.service;

import com.personaflow.commerce.behavior.entity.BehaviorEventEntity;
import com.personaflow.commerce.behavior.enums.BehaviorEventType;
import com.personaflow.commerce.behavior.vo.AgentProfileContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BehaviorContextServiceTest {

    @Mock
    private BehaviorQueryService behaviorQueryService;

    private BehaviorContextService behaviorContextService;

    @BeforeEach
    void setUp() {
        BehaviorSummaryService behaviorSummaryService = new BehaviorSummaryService(behaviorQueryService);
        behaviorContextService = new BehaviorContextService(behaviorQueryService, behaviorSummaryService);
    }

    @Test
    void buildAgentProfileContextAggregatesRecentBehavior() {
        when(behaviorQueryService.findEventsWithinDays(10001L, 30)).thenReturn(List.of(
                event("event-paid", BehaviorEventType.PAYMENT_SUCCESS, null, 30001L, 20001L, 201L, 50001L, new BigDecimal("918.00"), 5),
                event("event-order", BehaviorEventType.ORDER_CREATED, null, 30001L, 20001L, 201L, 50001L, new BigDecimal("918.00"), 4),
                event("event-cart", BehaviorEventType.CART_ADD, null, 30001L, 20001L, 201L, null, null, 3),
                event("event-view", BehaviorEventType.PRODUCT_VIEW, null, 30001L, 20001L, 201L, null, null, 2),
                event("event-search", BehaviorEventType.PRODUCT_SEARCH, "keyboard", null, null, 201L, null, null, 1)
        ));

        AgentProfileContext context = behaviorContextService.buildAgentProfileContext(10001L, 30);

        assertThat(context.userId()).isEqualTo(10001L);
        assertThat(context.recentEvents()).hasSize(5);
        assertThat(context.eventTypeCounts())
                .containsEntry("PAYMENT_SUCCESS", 1L)
                .containsEntry("CART_ADD", 1L)
                .containsEntry("PRODUCT_SEARCH", 1L);
        assertThat(context.recentKeywords()).containsExactly("keyboard");
        assertThat(context.topCategories().get(0).targetId()).isEqualTo(201L);
        assertThat(context.viewedProducts().get(0).targetId()).isEqualTo(20001L);
        assertThat(context.cartSignals()).hasSize(1);
        assertThat(context.orderSignals()).hasSize(1);
        assertThat(context.paidSignals()).hasSize(1);
        assertThat(context.fulfilledNeeds()).hasSize(1);
        assertThat(context.evidenceEventIds()).contains("event-paid", "event-view");
    }

    @Test
    void paymentSuccessIsFulfilledNeedAndSuppressesRepeatRecommendation() {
        when(behaviorQueryService.findEventsWithinDays(10001L, 30)).thenReturn(List.of(
                event("event-paid", BehaviorEventType.PAYMENT_SUCCESS, null, 30001L, 20001L, 201L, 50001L, new BigDecimal("918.00"), 5)
        ));

        AgentProfileContext context = behaviorContextService.buildAgentProfileContext(10001L, 30);

        assertThat(context.fulfilledNeeds()).hasSize(1);
        assertThat(context.fulfilledNeeds().get(0).skuId()).isEqualTo(30001L);
        assertThat(context.fulfilledNeeds().get(0).spuId()).isEqualTo(20001L);
        assertThat(context.fulfilledNeeds().get(0).preferenceConfirmed()).isTrue();
        assertThat(context.fulfilledNeeds().get(0).fulfilled()).isTrue();
        assertThat(context.fulfilledNeeds().get(0).complementTrigger()).isTrue();
        assertThat(context.fulfilledNeeds().get(0).repeatRecommendationSuppressed()).isTrue();
        assertThat(context.evidence().get(0).reason()).isEqualTo("fulfilled_need_and_complement_trigger");
    }

    private BehaviorEventEntity event(
            String eventId,
            BehaviorEventType eventType,
            String keyword,
            Long skuId,
            Long spuId,
            Long categoryId,
            Long orderId,
            BigDecimal amount,
            int minute
    ) {
        BehaviorEventEntity event = new BehaviorEventEntity();
        event.setEventId(eventId);
        event.setUserId(10001L);
        event.setEventType(eventType.name());
        event.setSourceModule("test");
        event.setObjectType(orderId == null ? "SKU" : "ORDER");
        event.setObjectId(orderId == null ? skuId : orderId);
        event.setKeyword(keyword);
        event.setSkuId(skuId);
        event.setSpuId(spuId);
        event.setCategoryId(categoryId);
        event.setOrderId(orderId);
        event.setAmount(amount);
        event.setPayloadJson("{}");
        event.setOccurredAt(LocalDateTime.of(2026, 6, 29, 10, minute));
        return event;
    }
}
