package com.personaflow.commerce.behavior.service;

import com.personaflow.commerce.behavior.entity.BehaviorEventEntity;
import com.personaflow.commerce.behavior.enums.BehaviorEventType;
import com.personaflow.commerce.behavior.vo.BehaviorSummaryVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class BehaviorSummaryServiceTest {

    @Mock
    private BehaviorQueryService behaviorQueryService;

    private BehaviorSummaryService behaviorSummaryService;

    @BeforeEach
    void setUp() {
        behaviorSummaryService = new BehaviorSummaryService(behaviorQueryService);
    }

    @Test
    void summarizeEventsAggregatesCountsKeywordsAndTargets() {
        List<BehaviorEventEntity> events = List.of(
                event("event-search", BehaviorEventType.PRODUCT_SEARCH, "keyboard", 30001L, 20001L, 201L, 0),
                event("event-view-1", BehaviorEventType.PRODUCT_VIEW, null, 30001L, 20001L, 201L, 1),
                event("event-view-2", BehaviorEventType.PRODUCT_VIEW, null, 30002L, 20001L, 201L, 2),
                event("event-cart", BehaviorEventType.CART_ADD, null, 30002L, 20001L, 201L, 3)
        );

        BehaviorSummaryVO summary = behaviorSummaryService.summarizeEvents(10001L, events);

        assertThat(summary.userId()).isEqualTo(10001L);
        assertThat(summary.eventTypeCounts())
                .containsEntry("PRODUCT_SEARCH", 1L)
                .containsEntry("PRODUCT_VIEW", 2L)
                .containsEntry("CART_ADD", 1L);
        assertThat(summary.recentKeywords()).containsExactly("keyboard");
        assertThat(summary.topCategories().get(0).targetId()).isEqualTo(201L);
        assertThat(summary.topCategories().get(0).count()).isEqualTo(4);
        assertThat(summary.topSpus().get(0).targetId()).isEqualTo(20001L);
    }

    private BehaviorEventEntity event(
            String eventId,
            BehaviorEventType eventType,
            String keyword,
            Long skuId,
            Long spuId,
            Long categoryId,
            int minute
    ) {
        BehaviorEventEntity event = new BehaviorEventEntity();
        event.setEventId(eventId);
        event.setUserId(10001L);
        event.setEventType(eventType.name());
        event.setKeyword(keyword);
        event.setSkuId(skuId);
        event.setSpuId(spuId);
        event.setCategoryId(categoryId);
        event.setOccurredAt(LocalDateTime.of(2026, 6, 29, 10, minute));
        return event;
    }
}
