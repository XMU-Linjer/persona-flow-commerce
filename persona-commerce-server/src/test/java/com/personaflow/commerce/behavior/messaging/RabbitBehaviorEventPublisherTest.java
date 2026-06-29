package com.personaflow.commerce.behavior.messaging;

import com.personaflow.commerce.behavior.enums.BehaviorEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RabbitBehaviorEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private RabbitBehaviorEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new RabbitBehaviorEventPublisher(rabbitTemplate, new BehaviorRoutingKeyResolver());
    }

    @Test
    void publishesPersistentMessageToBehaviorExchange() throws Exception {
        BehaviorEventMessage message = productViewMessage();

        publisher.publish(message);

        ArgumentCaptor<MessagePostProcessor> processorCaptor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(rabbitTemplate).convertAndSend(
                eq(BehaviorRabbitConfig.BEHAVIOR_EXCHANGE),
                eq("behavior.product.view"),
                same(message),
                processorCaptor.capture()
        );

        Message amqpMessage = new Message("{}".getBytes(StandardCharsets.UTF_8), new MessageProperties());
        processorCaptor.getValue().postProcessMessage(amqpMessage);
        assertThat(amqpMessage.getMessageProperties().getDeliveryMode()).isEqualTo(MessageDeliveryMode.PERSISTENT);
        assertThat(amqpMessage.getMessageProperties().getMessageId()).isEqualTo("message-001");
    }

    @Test
    void publishFailureIsExposedToCaller() {
        BehaviorEventMessage message = productViewMessage();
        doThrow(new AmqpException("broker unavailable"))
                .when(rabbitTemplate)
                .convertAndSend(
                        eq(BehaviorRabbitConfig.BEHAVIOR_EXCHANGE),
                        eq("behavior.product.view"),
                        same(message),
                        org.mockito.ArgumentMatchers.any(MessagePostProcessor.class)
                );

        assertThatThrownBy(() -> publisher.publish(message))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to publish behavior event");
    }

    private BehaviorEventMessage productViewMessage() {
        return new BehaviorEventMessage(
                "message-001",
                "event-001",
                BehaviorEventType.PRODUCT_VIEW.name(),
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
}
