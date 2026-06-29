package com.personaflow.commerce.behavior.vo;

import java.time.LocalDateTime;

public record AgentBehaviorSummary(
        String targetType,
        Long targetId,
        long count,
        LocalDateTime lastOccurredAt
) {
}
