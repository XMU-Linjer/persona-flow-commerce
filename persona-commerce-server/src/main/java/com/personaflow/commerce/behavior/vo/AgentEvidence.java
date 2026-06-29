package com.personaflow.commerce.behavior.vo;

import com.personaflow.commerce.behavior.entity.BehaviorEventEntity;

import java.time.LocalDateTime;

public record AgentEvidence(
        String eventId,
        String eventType,
        String reason,
        LocalDateTime occurredAt
) {

    public static AgentEvidence from(BehaviorEventEntity event) {
        return new AgentEvidence(
                event.getEventId(),
                event.getEventType(),
                evidenceReason(event),
                event.getOccurredAt()
        );
    }

    private static String evidenceReason(BehaviorEventEntity event) {
        if ("PAYMENT_SUCCESS".equals(event.getEventType())) {
            return "fulfilled_need_and_complement_trigger";
        }
        return "recent_behavior";
    }
}
