package com.personaflow.commerce.behavior.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.personaflow.commerce.behavior.entity.BehaviorConsumeLogEntity;
import com.personaflow.commerce.behavior.enums.BehaviorConsumeStatus;
import com.personaflow.commerce.behavior.mapper.BehaviorConsumeLogMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BehaviorConsumeLogService {

    private static final int MAX_ERROR_MESSAGE_LENGTH = 1000;

    private final BehaviorConsumeLogMapper behaviorConsumeLogMapper;

    public BehaviorConsumeLogService(BehaviorConsumeLogMapper behaviorConsumeLogMapper) {
        this.behaviorConsumeLogMapper = behaviorConsumeLogMapper;
    }

    @Transactional(readOnly = true)
    public BehaviorConsumeLogEntity findByMessageId(String messageId) {
        return behaviorConsumeLogMapper.selectOne(
                Wrappers.<BehaviorConsumeLogEntity>lambdaQuery()
                        .eq(BehaviorConsumeLogEntity::getMessageId, messageId)
        );
    }

    @Transactional(readOnly = true)
    public boolean isMessageSucceeded(String messageId) {
        BehaviorConsumeLogEntity log = findByMessageId(messageId);
        return log != null && Integer.valueOf(BehaviorConsumeStatus.SUCCESS.code()).equals(log.getStatus());
    }

    @Transactional
    public BehaviorConsumeLogEntity markProcessing(String messageId, String eventId) {
        return saveStatus(messageId, eventId, BehaviorConsumeStatus.PROCESSING, null, false);
    }

    @Transactional
    public BehaviorConsumeLogEntity markSuccess(String messageId, String eventId) {
        return saveStatus(messageId, eventId, BehaviorConsumeStatus.SUCCESS, null, false);
    }

    @Transactional
    public BehaviorConsumeLogEntity markFailed(String messageId, String eventId, String errorMessage) {
        return saveStatus(messageId, eventId, BehaviorConsumeStatus.FAILED, normalizeErrorMessage(errorMessage), true);
    }

    private BehaviorConsumeLogEntity saveStatus(
            String messageId,
            String eventId,
            BehaviorConsumeStatus status,
            String errorMessage,
            boolean incrementRetry
    ) {
        BehaviorConsumeLogEntity log = findByMessageId(messageId);
        if (log == null) {
            log = new BehaviorConsumeLogEntity();
            log.setMessageId(messageId);
            log.setEventId(eventId);
            log.setStatus(status.code());
            log.setRetryCount(incrementRetry ? 1 : 0);
            log.setErrorMessage(errorMessage);
            behaviorConsumeLogMapper.insert(log);
            return log;
        }

        log.setEventId(eventId);
        log.setStatus(status.code());
        log.setRetryCount(nextRetryCount(log.getRetryCount(), incrementRetry));
        log.setErrorMessage(errorMessage);
        behaviorConsumeLogMapper.updateById(log);
        return log;
    }

    private int nextRetryCount(Integer retryCount, boolean incrementRetry) {
        int currentRetryCount = retryCount == null ? 0 : retryCount;
        return incrementRetry ? currentRetryCount + 1 : currentRetryCount;
    }

    private String normalizeErrorMessage(String errorMessage) {
        if (errorMessage == null || errorMessage.length() <= MAX_ERROR_MESSAGE_LENGTH) {
            return errorMessage;
        }
        return errorMessage.substring(0, MAX_ERROR_MESSAGE_LENGTH);
    }
}
