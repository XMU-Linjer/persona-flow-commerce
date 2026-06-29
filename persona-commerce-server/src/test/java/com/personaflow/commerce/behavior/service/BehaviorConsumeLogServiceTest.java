package com.personaflow.commerce.behavior.service;

import com.personaflow.commerce.behavior.entity.BehaviorConsumeLogEntity;
import com.personaflow.commerce.behavior.enums.BehaviorConsumeStatus;
import com.personaflow.commerce.behavior.mapper.BehaviorConsumeLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BehaviorConsumeLogServiceTest {

    @Mock
    private BehaviorConsumeLogMapper behaviorConsumeLogMapper;

    private BehaviorConsumeLogService consumeLogService;

    @BeforeEach
    void setUp() {
        consumeLogService = new BehaviorConsumeLogService(behaviorConsumeLogMapper);
    }

    @Test
    void markProcessingInsertsProcessingConsumeLog() {
        when(behaviorConsumeLogMapper.selectOne(any())).thenReturn(null);

        consumeLogService.markProcessing("message-001", "event-001");

        ArgumentCaptor<BehaviorConsumeLogEntity> logCaptor = ArgumentCaptor.forClass(BehaviorConsumeLogEntity.class);
        verify(behaviorConsumeLogMapper).insert(logCaptor.capture());
        BehaviorConsumeLogEntity inserted = logCaptor.getValue();
        assertThat(inserted.getMessageId()).isEqualTo("message-001");
        assertThat(inserted.getEventId()).isEqualTo("event-001");
        assertThat(inserted.getStatus()).isEqualTo(BehaviorConsumeStatus.PROCESSING.code());
        assertThat(inserted.getRetryCount()).isZero();
        assertThat(inserted.getErrorMessage()).isNull();
    }

    @Test
    void markSuccessUpdatesExistingConsumeLog() {
        BehaviorConsumeLogEntity existingLog = existingLog(BehaviorConsumeStatus.PROCESSING, 1, "previous error");
        when(behaviorConsumeLogMapper.selectOne(any())).thenReturn(existingLog);

        consumeLogService.markSuccess("message-001", "event-001");

        verify(behaviorConsumeLogMapper).updateById(existingLog);
        assertThat(existingLog.getStatus()).isEqualTo(BehaviorConsumeStatus.SUCCESS.code());
        assertThat(existingLog.getRetryCount()).isEqualTo(1);
        assertThat(existingLog.getErrorMessage()).isNull();
    }

    @Test
    void markFailedUpdatesExistingConsumeLogAndIncrementsRetryCount() {
        BehaviorConsumeLogEntity existingLog = existingLog(BehaviorConsumeStatus.PROCESSING, 1, null);
        when(behaviorConsumeLogMapper.selectOne(any())).thenReturn(existingLog);

        consumeLogService.markFailed("message-001", "event-001", "database unavailable");

        verify(behaviorConsumeLogMapper).updateById(existingLog);
        assertThat(existingLog.getStatus()).isEqualTo(BehaviorConsumeStatus.FAILED.code());
        assertThat(existingLog.getRetryCount()).isEqualTo(2);
        assertThat(existingLog.getErrorMessage()).isEqualTo("database unavailable");
    }

    private BehaviorConsumeLogEntity existingLog(
            BehaviorConsumeStatus status,
            Integer retryCount,
            String errorMessage
    ) {
        BehaviorConsumeLogEntity log = new BehaviorConsumeLogEntity();
        log.setId(1L);
        log.setMessageId("message-001");
        log.setEventId("event-001");
        log.setStatus(status.code());
        log.setRetryCount(retryCount);
        log.setErrorMessage(errorMessage);
        return log;
    }
}
