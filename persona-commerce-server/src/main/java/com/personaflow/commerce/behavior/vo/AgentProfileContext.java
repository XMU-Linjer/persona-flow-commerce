package com.personaflow.commerce.behavior.vo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record AgentProfileContext(
        Long userId,
        List<AgentBehaviorEvent> recentEvents,
        Map<String, Long> eventTypeCounts,
        List<String> recentKeywords,
        List<AgentBehaviorSummary> topCategories,
        List<AgentBehaviorSummary> viewedProducts,
        List<AgentDemandSignal> cartSignals,
        List<AgentDemandSignal> orderSignals,
        List<AgentDemandSignal> paidSignals,
        List<AgentDemandSignal> canceledSignals,
        List<AgentDemandSignal> fulfilledNeeds,
        List<String> evidenceEventIds,
        List<AgentEvidence> evidence,
        LocalDateTime generatedAt
) {
}
