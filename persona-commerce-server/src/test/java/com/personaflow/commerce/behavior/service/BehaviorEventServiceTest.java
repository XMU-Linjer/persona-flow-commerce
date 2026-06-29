package com.personaflow.commerce.behavior.service;

import com.personaflow.commerce.behavior.dto.BehaviorEventCreateCommand;
import com.personaflow.commerce.behavior.entity.BehaviorEventEntity;
import com.personaflow.commerce.behavior.enums.BehaviorEventType;
import com.personaflow.commerce.behavior.mapper.BehaviorEventMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BehaviorEventServiceTest {

    @Mock
    private BehaviorEventMapper behaviorEventMapper;

    private BehaviorEventService behaviorEventService;

    @BeforeEach
    void setUp() {
        behaviorEventService = new BehaviorEventService(behaviorEventMapper);
    }

    @Test
    void saveEventInsertsBehaviorEvent() {
        when(behaviorEventMapper.selectOne(any())).thenReturn(null);
        when(behaviorEventMapper.insert(any(BehaviorEventEntity.class))).thenAnswer(invocation -> {
            BehaviorEventEntity event = invocation.getArgument(0);
            event.setId(1L);
            return 1;
        });

        BehaviorEventEntity result = behaviorEventService.saveEvent(productViewCommand());

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEventId()).isEqualTo("event-001");
        assertThat(result.getEventType()).isEqualTo("PRODUCT_VIEW");

        ArgumentCaptor<BehaviorEventEntity> eventCaptor = ArgumentCaptor.forClass(BehaviorEventEntity.class);
        verify(behaviorEventMapper).insert(eventCaptor.capture());
        BehaviorEventEntity inserted = eventCaptor.getValue();
        assertThat(inserted.getUserId()).isEqualTo(10001L);
        assertThat(inserted.getSourceModule()).isEqualTo("catalog");
        assertThat(inserted.getObjectType()).isEqualTo("SKU");
        assertThat(inserted.getSkuId()).isEqualTo(30001L);
        assertThat(inserted.getSpuId()).isEqualTo(20001L);
        assertThat(inserted.getCategoryId()).isEqualTo(201L);
        assertThat(inserted.getOccurredAt()).isEqualTo(LocalDateTime.of(2026, 6, 29, 10, 0));
    }

    @Test
    void saveEventReturnsExistingEventWhenEventIdAlreadyExists() {
        BehaviorEventEntity existingEvent = eventEntity("event-001", BehaviorEventType.PRODUCT_VIEW);
        when(behaviorEventMapper.selectOne(any())).thenReturn(existingEvent);

        BehaviorEventEntity result = behaviorEventService.saveEvent(productViewCommand());

        assertThat(result).isSameAs(existingEvent);
        verify(behaviorEventMapper, never()).insert(any(BehaviorEventEntity.class));
    }

    @Test
    void saveEventTreatsConcurrentDuplicateKeyAsIdempotentSuccess() {
        BehaviorEventEntity existingEvent = eventEntity("event-001", BehaviorEventType.PRODUCT_VIEW);
        when(behaviorEventMapper.selectOne(any()))
                .thenReturn(null)
                .thenReturn(existingEvent);
        when(behaviorEventMapper.insert(any(BehaviorEventEntity.class)))
                .thenThrow(new DuplicateKeyException("duplicate event"));

        BehaviorEventEntity result = behaviorEventService.saveEvent(productViewCommand());

        assertThat(result).isSameAs(existingEvent);
    }

    @Test
    void findRecentEventsReturnsEventsForUser() {
        List<BehaviorEventEntity> events = List.of(
                eventEntity("event-002", BehaviorEventType.CART_ADD),
                eventEntity("event-001", BehaviorEventType.PRODUCT_VIEW)
        );
        when(behaviorEventMapper.selectList(any())).thenReturn(events);

        List<BehaviorEventEntity> result = behaviorEventService.findRecentEvents(10001L, 10);

        assertThat(result).containsExactlyElementsOf(events);
        verify(behaviorEventMapper).selectList(any());
    }

    @Test
    void savePaymentSuccessEventKeepsPayloadJson() {
        when(behaviorEventMapper.selectOne(any())).thenReturn(null);
        when(behaviorEventMapper.insert(any(BehaviorEventEntity.class))).thenReturn(1);

        behaviorEventService.saveEvent(paymentSuccessCommand());

        ArgumentCaptor<BehaviorEventEntity> eventCaptor = ArgumentCaptor.forClass(BehaviorEventEntity.class);
        verify(behaviorEventMapper).insert(eventCaptor.capture());
        BehaviorEventEntity inserted = eventCaptor.getValue();
        assertThat(inserted.getEventType()).isEqualTo("PAYMENT_SUCCESS");
        assertThat(inserted.getPayloadJson()).contains("preferenceConfirmed");
        assertThat(inserted.getPayloadJson()).contains("needFulfilled");
        assertThat(inserted.getPayloadJson()).contains("complementTriggered");
        assertThat(inserted.getAmount()).isEqualByComparingTo("918.00");
    }

    private BehaviorEventCreateCommand productViewCommand() {
        return new BehaviorEventCreateCommand(
                "event-001",
                10001L,
                BehaviorEventType.PRODUCT_VIEW,
                "catalog",
                "SKU",
                30001L,
                null,
                30001L,
                20001L,
                201L,
                null,
                null,
                "{\"scene\":\"detail\"}",
                LocalDateTime.of(2026, 6, 29, 10, 0)
        );
    }

    private BehaviorEventCreateCommand paymentSuccessCommand() {
        return new BehaviorEventCreateCommand(
                "event-payment-001",
                10001L,
                BehaviorEventType.PAYMENT_SUCCESS,
                "trade",
                "ORDER",
                50001L,
                null,
                30001L,
                20001L,
                201L,
                50001L,
                new BigDecimal("918.00"),
                "{\"preferenceConfirmed\":true,\"needFulfilled\":true,\"complementTriggered\":true}",
                LocalDateTime.of(2026, 6, 29, 11, 0)
        );
    }

    private BehaviorEventEntity eventEntity(String eventId, BehaviorEventType eventType) {
        BehaviorEventEntity event = new BehaviorEventEntity();
        event.setId(1L);
        event.setEventId(eventId);
        event.setUserId(10001L);
        event.setEventType(eventType.name());
        event.setSourceModule("catalog");
        event.setOccurredAt(LocalDateTime.of(2026, 6, 29, 10, 0));
        return event;
    }
}
