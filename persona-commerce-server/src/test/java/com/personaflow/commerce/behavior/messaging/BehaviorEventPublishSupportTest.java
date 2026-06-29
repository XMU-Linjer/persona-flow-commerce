package com.personaflow.commerce.behavior.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.personaflow.commerce.behavior.enums.BehaviorEventType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BehaviorEventPublishSupportTest {

    @Mock
    private BehaviorEventPublisher behaviorEventPublisher;

    private BehaviorEventPublishSupport publishSupport;

    @BeforeEach
    void setUp() {
        publishSupport = new BehaviorEventPublishSupport(behaviorEventPublisher, new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void publishBuildsMessageAndDelegatesToPublisher() {
        publishSupport.publish(command());

        ArgumentCaptor<BehaviorEventMessage> messageCaptor = ArgumentCaptor.forClass(BehaviorEventMessage.class);
        verify(behaviorEventPublisher).publish(messageCaptor.capture());
        BehaviorEventMessage message = messageCaptor.getValue();
        assertThat(message.messageId()).isNotBlank();
        assertThat(message.eventId()).isNotBlank();
        assertThat(message.eventType()).isEqualTo("PRODUCT_VIEW");
        assertThat(message.userId()).isEqualTo(10001L);
        assertThat(message.sourceModule()).isEqualTo("catalog");
        assertThat(message.payloadJson()).contains("KeyForge K3");
        assertThat(message.version()).isEqualTo("1.0");
    }

    @Test
    void publisherFailureDoesNotEscapeToMainBusiness() {
        doThrow(new IllegalStateException("broker unavailable"))
                .when(behaviorEventPublisher)
                .publish(any(BehaviorEventMessage.class));

        assertThatNoException().isThrownBy(() -> publishSupport.publish(command()));
    }

    @Test
    void publishAfterCommitDefersPublishWhenTransactionSynchronizationIsActive() {
        TransactionSynchronizationManager.initSynchronization();

        publishSupport.publishAfterCommit(command());

        verify(behaviorEventPublisher, never()).publish(any(BehaviorEventMessage.class));
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);
        verify(behaviorEventPublisher).publish(any(BehaviorEventMessage.class));
    }

    private BehaviorEventPublishCommand command() {
        return new BehaviorEventPublishCommand(
                BehaviorEventType.PRODUCT_VIEW,
                10001L,
                "catalog",
                "SPU",
                20001L,
                null,
                30001L,
                20001L,
                201L,
                null,
                null,
                Map.of("productName", "KeyForge K3")
        );
    }
}
