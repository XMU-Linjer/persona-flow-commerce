package com.personaflow.commerce.behavior.service;

import com.personaflow.commerce.behavior.entity.BehaviorEventEntity;
import com.personaflow.commerce.behavior.enums.BehaviorEventType;
import com.personaflow.commerce.behavior.vo.AgentBehaviorEvent;
import com.personaflow.commerce.behavior.vo.AgentBehaviorSummary;
import com.personaflow.commerce.behavior.vo.AgentDemandSignal;
import com.personaflow.commerce.behavior.vo.AgentEvidence;
import com.personaflow.commerce.behavior.vo.AgentProfileContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BehaviorContextService {

    private final BehaviorQueryService behaviorQueryService;
    private final BehaviorSummaryService behaviorSummaryService;

    public BehaviorContextService(
            BehaviorQueryService behaviorQueryService,
            BehaviorSummaryService behaviorSummaryService
    ) {
        this.behaviorQueryService = behaviorQueryService;
        this.behaviorSummaryService = behaviorSummaryService;
    }

    @Transactional(readOnly = true)
    public AgentProfileContext buildAgentProfileContext(Long userId, Integer days) {
        List<BehaviorEventEntity> events = behaviorQueryService.findEventsWithinDays(userId, days);
        List<AgentDemandSignal> paidSignals = demandSignals(events, BehaviorEventType.PAYMENT_SUCCESS);

        return new AgentProfileContext(
                userId,
                events.stream().map(AgentBehaviorEvent::from).toList(),
                behaviorSummaryService.eventTypeCounts(events),
                behaviorSummaryService.recentKeywords(events),
                behaviorSummaryService.summarizeTargets(events, BehaviorEventEntity::getCategoryId, "CATEGORY"),
                viewedProducts(events),
                cartSignals(events),
                demandSignals(events, BehaviorEventType.ORDER_CREATED),
                paidSignals,
                demandSignals(events, BehaviorEventType.ORDER_CANCELED),
                paidSignals,
                events.stream().map(BehaviorEventEntity::getEventId).toList(),
                events.stream().map(AgentEvidence::from).toList(),
                LocalDateTime.now()
        );
    }

    private List<AgentBehaviorSummary> viewedProducts(List<BehaviorEventEntity> events) {
        return behaviorSummaryService.summarizeTargets(
                events.stream()
                        .filter(event -> BehaviorEventType.PRODUCT_VIEW.name().equals(event.getEventType()))
                        .toList(),
                event -> event.getSpuId() != null ? event.getSpuId() : event.getSkuId(),
                "PRODUCT"
        );
    }

    private List<AgentDemandSignal> cartSignals(List<BehaviorEventEntity> events) {
        return events.stream()
                .filter(event -> BehaviorEventType.CART_ADD.name().equals(event.getEventType())
                        || BehaviorEventType.CART_REMOVE.name().equals(event.getEventType())
                        || BehaviorEventType.CART_CLEAR.name().equals(event.getEventType()))
                .map(AgentDemandSignal::from)
                .toList();
    }

    private List<AgentDemandSignal> demandSignals(
            List<BehaviorEventEntity> events,
            BehaviorEventType eventType
    ) {
        return events.stream()
                .filter(event -> eventType.name().equals(event.getEventType()))
                .map(AgentDemandSignal::from)
                .toList();
    }
}
