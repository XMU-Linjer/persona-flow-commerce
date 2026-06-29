package com.personaflow.commerce.behavior.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.personaflow.commerce.behavior.entity.BehaviorEventEntity;
import com.personaflow.commerce.behavior.enums.BehaviorEventType;
import com.personaflow.commerce.behavior.mapper.BehaviorEventMapper;
import com.personaflow.commerce.behavior.vo.BehaviorEventVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BehaviorQueryService {

    private static final int DEFAULT_EVENT_LIMIT = 50;
    private static final int MAX_EVENT_LIMIT = 200;
    private static final int DEFAULT_CONTEXT_DAYS = 30;
    private static final int MAX_CONTEXT_DAYS = 90;
    private static final int MAX_CONTEXT_EVENTS = 1000;

    private final BehaviorEventMapper behaviorEventMapper;

    public BehaviorQueryService(BehaviorEventMapper behaviorEventMapper) {
        this.behaviorEventMapper = behaviorEventMapper;
    }

    @Transactional(readOnly = true)
    public List<BehaviorEventVO> findRecentEvents(Long userId, BehaviorEventType eventType, Integer limit) {
        return findRecentEventEntities(userId, eventType, limit)
                .stream()
                .map(BehaviorEventVO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BehaviorEventEntity> findRecentEventEntities(Long userId, BehaviorEventType eventType, Integer limit) {
        LambdaQueryWrapper<BehaviorEventEntity> query = Wrappers.<BehaviorEventEntity>lambdaQuery()
                .eq(BehaviorEventEntity::getUserId, userId);
        if (eventType != null) {
            query.eq(BehaviorEventEntity::getEventType, eventType.name());
        }
        return behaviorEventMapper.selectList(query
                .orderByDesc(BehaviorEventEntity::getOccurredAt)
                .orderByDesc(BehaviorEventEntity::getId)
                .last("LIMIT " + normalizeEventLimit(limit)));
    }

    @Transactional(readOnly = true)
    public List<BehaviorEventEntity> findEventsWithinDays(Long userId, Integer days) {
        LocalDateTime since = LocalDateTime.now().minusDays(normalizeContextDays(days));
        return behaviorEventMapper.selectList(
                Wrappers.<BehaviorEventEntity>lambdaQuery()
                        .eq(BehaviorEventEntity::getUserId, userId)
                        .ge(BehaviorEventEntity::getOccurredAt, since)
                        .orderByDesc(BehaviorEventEntity::getOccurredAt)
                        .orderByDesc(BehaviorEventEntity::getId)
                        .last("LIMIT " + MAX_CONTEXT_EVENTS)
        );
    }

    int normalizeEventLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return DEFAULT_EVENT_LIMIT;
        }
        return Math.min(limit, MAX_EVENT_LIMIT);
    }

    int normalizeContextDays(Integer days) {
        if (days == null || days < 1) {
            return DEFAULT_CONTEXT_DAYS;
        }
        return Math.min(days, MAX_CONTEXT_DAYS);
    }
}
