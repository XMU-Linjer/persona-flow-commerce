package com.personaflow.commerce.behavior.vo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record BehaviorSummaryVO(
        Long userId,
        Map<String, Long> eventTypeCounts,
        List<String> recentKeywords,
        List<AgentBehaviorSummary> topCategories,
        List<AgentBehaviorSummary> topSkus,
        List<AgentBehaviorSummary> topSpus,
        LocalDateTime generatedAt
) {
}
