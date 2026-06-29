package com.personaflow.commerce.behavior.messaging;

import com.personaflow.commerce.behavior.dto.BehaviorEventCreateCommand;
import com.personaflow.commerce.behavior.entity.BehaviorEventEntity;
import com.personaflow.commerce.behavior.enums.BehaviorEventType;
import com.personaflow.commerce.behavior.service.BehaviorConsumeLogService;
import com.personaflow.commerce.behavior.service.BehaviorEventService;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BehaviorEventConsumerTest {

    @Mock
    private BehaviorEventService behaviorEventService;

    @Mock
    private BehaviorConsumeLogService consumeLogService;

    @Mock
    private Channel channel;

    private BehaviorEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new BehaviorEventConsumer(behaviorEventService, consumeLogService);
    }

    @Test
    void consumesProductViewAndPersistsBehaviorEvent() throws IOException {
        BehaviorEventMessage message = productViewMessage();
        when(consumeLogService.isMessageSucceeded("message-001")).thenReturn(false);
        when(behaviorEventService.saveEvent(any(BehaviorEventCreateCommand.class))).thenReturn(new BehaviorEventEntity());

        consumer.consume(message, amqpMessage(7L), channel);

        ArgumentCaptor<BehaviorEventCreateCommand> commandCaptor = ArgumentCaptor.forClass(BehaviorEventCreateCommand.class);
        verify(consumeLogService).markProcessing("message-001", "event-001");
        verify(behaviorEventService).saveEvent(commandCaptor.capture());
        verify(consumeLogService).markSuccess("message-001", "event-001");
        verify(channel).basicAck(7L, false);

        BehaviorEventCreateCommand command = commandCaptor.getValue();
        assertThat(command.eventType()).isEqualTo(BehaviorEventType.PRODUCT_VIEW);
        assertThat(command.userId()).isEqualTo(10001L);
        assertThat(command.skuId()).isEqualTo(30001L);
        assertThat(command.payloadJson()).contains("detail");
    }

    @Test
    void consumesPaymentSuccessAndKeepsPayloadJson() throws IOException {
        BehaviorEventMessage message = paymentSuccessMessage();
        when(consumeLogService.isMessageSucceeded("message-payment-001")).thenReturn(false);
        when(behaviorEventService.saveEvent(any(BehaviorEventCreateCommand.class))).thenReturn(new BehaviorEventEntity());

        consumer.consume(message, amqpMessage(8L), channel);

        ArgumentCaptor<BehaviorEventCreateCommand> commandCaptor = ArgumentCaptor.forClass(BehaviorEventCreateCommand.class);
        verify(behaviorEventService).saveEvent(commandCaptor.capture());
        BehaviorEventCreateCommand command = commandCaptor.getValue();
        assertThat(command.eventType()).isEqualTo(BehaviorEventType.PAYMENT_SUCCESS);
        assertThat(command.payloadJson()).contains("preferenceConfirmed");
        assertThat(command.payloadJson()).contains("needFulfilled");
        assertThat(command.payloadJson()).contains("complementTriggered");
        assertThat(command.amount()).isEqualByComparingTo("918.00");
        verify(channel).basicAck(8L, false);
    }

    @Test
    void duplicateMessageIdAlreadySucceededIsAcknowledgedWithoutSavingAgain() throws IOException {
        BehaviorEventMessage message = productViewMessage();
        when(consumeLogService.isMessageSucceeded("message-001")).thenReturn(true);

        consumer.consume(message, amqpMessage(9L), channel);

        verify(behaviorEventService, never()).saveEvent(any(BehaviorEventCreateCommand.class));
        verify(consumeLogService, never()).markProcessing(any(), any());
        verify(consumeLogService, never()).markSuccess(any(), any());
        verify(channel).basicAck(9L, false);
    }

    @Test
    void duplicateEventIdDelegatesToIdempotentBehaviorEventServiceAndMarksSuccess() throws IOException {
        BehaviorEventMessage message = productViewMessage("message-duplicate", "event-001");
        BehaviorEventEntity existingEvent = new BehaviorEventEntity();
        existingEvent.setEventId("event-001");
        when(consumeLogService.isMessageSucceeded("message-duplicate")).thenReturn(false);
        when(behaviorEventService.saveEvent(any(BehaviorEventCreateCommand.class))).thenReturn(existingEvent);

        consumer.consume(message, amqpMessage(10L), channel);

        verify(behaviorEventService).saveEvent(any(BehaviorEventCreateCommand.class));
        verify(consumeLogService).markSuccess("message-duplicate", "event-001");
        verify(channel).basicAck(10L, false);
    }

    @Test
    void invalidEventTypeRecordsFailureAndLetsRabbitRetryOrDeadLetter() throws IOException {
        BehaviorEventMessage message = productViewMessage("message-invalid", "event-invalid", "INVALID_EVENT");
        when(consumeLogService.isMessageSucceeded("message-invalid")).thenReturn(false);

        assertThatThrownBy(() -> consumer.consume(message, amqpMessage(11L), channel))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported behavior event type");

        verify(consumeLogService).markFailed(eq("message-invalid"), eq("event-invalid"), contains("Unsupported"));
        verify(behaviorEventService, never()).saveEvent(any(BehaviorEventCreateCommand.class));
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    @Test
    void saveFailureRecordsFailedConsumeLog() throws IOException {
        BehaviorEventMessage message = productViewMessage();
        when(consumeLogService.isMessageSucceeded("message-001")).thenReturn(false);
        doThrow(new IllegalStateException("database unavailable"))
                .when(behaviorEventService)
                .saveEvent(any(BehaviorEventCreateCommand.class));

        assertThatThrownBy(() -> consumer.consume(message, amqpMessage(12L), channel))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("database unavailable");

        verify(consumeLogService).markProcessing("message-001", "event-001");
        verify(consumeLogService).markFailed("message-001", "event-001", "database unavailable");
        verify(consumeLogService, never()).markSuccess(any(), any());
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    private Message amqpMessage(long deliveryTag) {
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(deliveryTag);
        return new Message(new byte[0], properties);
    }

    private BehaviorEventMessage productViewMessage() {
        return productViewMessage("message-001", "event-001");
    }

    private BehaviorEventMessage productViewMessage(String messageId, String eventId) {
        return productViewMessage(messageId, eventId, BehaviorEventType.PRODUCT_VIEW.name());
    }

    private BehaviorEventMessage productViewMessage(String messageId, String eventId, String eventType) {
        return new BehaviorEventMessage(
                messageId,
                eventId,
                eventType,
                10001L,
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
                LocalDateTime.of(2026, 6, 29, 10, 0),
                "trace-001",
                "1.0"
        );
    }

    private BehaviorEventMessage paymentSuccessMessage() {
        return new BehaviorEventMessage(
                "message-payment-001",
                "event-payment-001",
                BehaviorEventType.PAYMENT_SUCCESS.name(),
                10001L,
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
                LocalDateTime.of(2026, 6, 29, 11, 0),
                "trace-payment-001",
                "1.0"
        );
    }
}
