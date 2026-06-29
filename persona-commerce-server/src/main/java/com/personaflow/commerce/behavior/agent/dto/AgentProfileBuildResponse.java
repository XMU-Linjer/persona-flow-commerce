package com.personaflow.commerce.behavior.agent.dto;

import java.util.Map;

public record AgentProfileBuildResponse(
        String workflowId,
        Map<String, Object> behaviorFactReport,
        Map<String, Object> intentReport,
        Map<String, Object> trendReport,
        Map<String, Object> profileDraft,
        Map<String, Object> auditReport,
        AgentProfileResult profile
) {
}
