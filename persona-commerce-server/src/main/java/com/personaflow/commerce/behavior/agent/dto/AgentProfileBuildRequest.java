package com.personaflow.commerce.behavior.agent.dto;

import com.personaflow.commerce.behavior.vo.AgentBehaviorEvent;
import com.personaflow.commerce.behavior.vo.AgentBehaviorSummary;
import com.personaflow.commerce.behavior.vo.AgentDemandSignal;
import com.personaflow.commerce.behavior.vo.AgentEvidence;
import com.personaflow.commerce.behavior.vo.AgentProfileContext;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record AgentProfileBuildRequest(
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

    public static AgentProfileBuildRequest from(AgentProfileContext context) {
        return new AgentProfileBuildRequest(
                context.userId(),
                context.recentEvents(),
                context.eventTypeCounts(),
                context.recentKeywords(),
                context.topCategories(),
                context.viewedProducts(),
                context.cartSignals(),
                context.orderSignals(),
                context.paidSignals(),
                context.canceledSignals(),
                context.fulfilledNeeds(),
                context.evidenceEventIds(),
                context.evidence(),
                context.generatedAt()
        );
    }
}
