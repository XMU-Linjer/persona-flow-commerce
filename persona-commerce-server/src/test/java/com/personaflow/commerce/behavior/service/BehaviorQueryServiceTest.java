package com.personaflow.commerce.behavior.service;

import com.personaflow.commerce.behavior.entity.BehaviorEventEntity;
import com.personaflow.commerce.behavior.enums.BehaviorEventType;
import com.personaflow.commerce.behavior.mapper.BehaviorEventMapper;
import com.personaflow.commerce.behavior.vo.BehaviorEventVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BehaviorQueryServiceTest {

    @Mock
    private BehaviorEventMapper behaviorEventMapper;

    private BehaviorQueryService behaviorQueryService;

    @BeforeEach
    void setUp() {
        behaviorQueryService = new BehaviorQueryService(behaviorEventMapper);
    }

    @Test
    void findRecentEventsMapsBehaviorEvents() {
        when(behaviorEventMapper.selectList(any())).thenReturn(List.of(event(
                "event-view",
                BehaviorEventType.PRODUCT_VIEW,
                10001L,
                "keyboard",
                30001L,
                20001L,
                201L
        )));

        List<BehaviorEventVO> result = behaviorQueryService.findRecentEvents(
                10001L,
                BehaviorEventType.PRODUCT_VIEW,
                50
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).eventId()).isEqualTo("event-view");
        assertThat(result.get(0).eventType()).isEqualTo("PRODUCT_VIEW");
        assertThat(result.get(0).skuId()).isEqualTo(30001L);
    }

    @Test
    void findRecentEventsClampsLimitToMax() {
        when(behaviorEventMapper.selectList(any())).thenReturn(List.of());

        behaviorQueryService.findRecentEvents(10001L, null, 500);

        assertThat(behaviorQueryService.normalizeEventLimit(500)).isEqualTo(200);
        verify(behaviorEventMapper).selectList(any());
    }

    @Test
    void findEventsWithinDaysClampsDaysToMax() {
        when(behaviorEventMapper.selectList(any())).thenReturn(List.of());

        behaviorQueryService.findEventsWithinDays(10001L, 120);

        assertThat(behaviorQueryService.normalizeContextDays(120)).isEqualTo(90);
        verify(behaviorEventMapper).selectList(any());
    }

    private BehaviorEventEntity event(
            String eventId,
            BehaviorEventType eventType,
            Long userId,
            String keyword,
            Long skuId,
            Long spuId,
            Long categoryId
    ) {
        BehaviorEventEntity event = new BehaviorEventEntity();
        event.setId(1L);
        event.setEventId(eventId);
        event.setUserId(userId);
        event.setEventType(eventType.name());
        event.setSourceModule("catalog");
        event.setObjectType("SKU");
        event.setObjectId(skuId);
        event.setKeyword(keyword);
        event.setSkuId(skuId);
        event.setSpuId(spuId);
        event.setCategoryId(categoryId);
        event.setOccurredAt(LocalDateTime.of(2026, 6, 29, 10, 0));
        return event;
    }
}
