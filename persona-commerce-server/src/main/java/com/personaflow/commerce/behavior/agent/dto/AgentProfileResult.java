package com.personaflow.commerce.behavior.agent.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record AgentProfileResult(
        String artifactId,
        String workflowId,
        Long userId,
        String artifactType,
        String createdAt,
        BigDecimal confidence,
        List<String> evidenceEventIds,
        Integer versionNo,
        String summary,
        Map<String, Object> profile
) {
}
