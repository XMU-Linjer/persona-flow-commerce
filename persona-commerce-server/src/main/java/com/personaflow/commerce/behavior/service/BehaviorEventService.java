package com.personaflow.commerce.behavior.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.personaflow.commerce.behavior.dto.BehaviorEventCreateCommand;
import com.personaflow.commerce.behavior.entity.BehaviorEventEntity;
import com.personaflow.commerce.behavior.mapper.BehaviorEventMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BehaviorEventService {

    private static final int DEFAULT_RECENT_LIMIT = 20;
    private static final int MAX_RECENT_LIMIT = 200;

    private final BehaviorEventMapper behaviorEventMapper;

    public BehaviorEventService(BehaviorEventMapper behaviorEventMapper) {
        this.behaviorEventMapper = behaviorEventMapper;
    }

    @Transactional
    public BehaviorEventEntity saveEvent(BehaviorEventCreateCommand command) {
        BehaviorEventEntity existingEvent = findByEventId(command.eventId());
        if (existingEvent != null) {
            return existingEvent;
        }

        BehaviorEventEntity event = toEntity(command);
        try {
            behaviorEventMapper.insert(event);
            return event;
        } catch (DuplicateKeyException exception) {
            BehaviorEventEntity concurrentEvent = findByEventId(command.eventId());
            if (concurrentEvent != null) {
                return concurrentEvent;
            }
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<BehaviorEventEntity> findRecentEvents(Long userId, Integer limit) {
        return behaviorEventMapper.selectList(
                Wrappers.<BehaviorEventEntity>lambdaQuery()
                        .eq(BehaviorEventEntity::getUserId, userId)
                        .orderByDesc(BehaviorEventEntity::getOccurredAt)
                        .orderByDesc(BehaviorEventEntity::getId)
                        .last("LIMIT " + normalizeLimit(limit))
        );
    }

    private BehaviorEventEntity findByEventId(String eventId) {
        return behaviorEventMapper.selectOne(
                Wrappers.<BehaviorEventEntity>lambdaQuery()
                        .eq(BehaviorEventEntity::getEventId, eventId)
        );
    }

    private BehaviorEventEntity toEntity(BehaviorEventCreateCommand command) {
        BehaviorEventEntity event = new BehaviorEventEntity();
        event.setEventId(command.eventId());
        event.setUserId(command.userId());
        event.setEventType(command.eventType().name());
        event.setSourceModule(command.sourceModule());
        event.setObjectType(command.objectType());
        event.setObjectId(command.objectId());
        event.setKeyword(command.keyword());
        event.setSkuId(command.skuId());
        event.setSpuId(command.spuId());
        event.setCategoryId(command.categoryId());
        event.setOrderId(command.orderId());
        event.setAmount(command.amount());
        event.setPayloadJson(command.payloadJson());
        event.setOccurredAt(command.occurredAt() == null ? LocalDateTime.now() : command.occurredAt());
        return event;
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return DEFAULT_RECENT_LIMIT;
        }
        return Math.min(limit, MAX_RECENT_LIMIT);
    }
}
