package com.personaflow.commerce.behavior.service;

import com.personaflow.commerce.behavior.entity.BehaviorEventEntity;
import com.personaflow.commerce.behavior.vo.AgentBehaviorSummary;
import com.personaflow.commerce.behavior.vo.BehaviorSummaryVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BehaviorSummaryService {

    private static final int MAX_SUMMARY_ITEMS = 10;

    private final BehaviorQueryService behaviorQueryService;

    public BehaviorSummaryService(BehaviorQueryService behaviorQueryService) {
        this.behaviorQueryService = behaviorQueryService;
    }

    @Transactional(readOnly = true)
    public BehaviorSummaryVO summarize(Long userId, Integer days) {
        return summarizeEvents(userId, behaviorQueryService.findEventsWithinDays(userId, days));
    }

    public BehaviorSummaryVO summarizeEvents(Long userId, List<BehaviorEventEntity> events) {
        return new BehaviorSummaryVO(
                userId,
                eventTypeCounts(events),
                recentKeywords(events),
                summarizeTargets(events, BehaviorEventEntity::getCategoryId, "CATEGORY"),
                summarizeTargets(events, BehaviorEventEntity::getSkuId, "SKU"),
                summarizeTargets(events, BehaviorEventEntity::getSpuId, "SPU"),
                LocalDateTime.now()
        );
    }

    public Map<String, Long> eventTypeCounts(List<BehaviorEventEntity> events) {
        return events.stream()
                .filter(event -> event.getEventType() != null)
                .collect(Collectors.groupingBy(
                        BehaviorEventEntity::getEventType,
                        LinkedHashMap::new,
                        Collectors.counting()
                ));
    }

    public List<String> recentKeywords(List<BehaviorEventEntity> events) {
        return events.stream()
                .map(BehaviorEventEntity::getKeyword)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(keyword -> !keyword.isBlank())
                .distinct()
                .limit(MAX_SUMMARY_ITEMS)
                .toList();
    }

    public List<AgentBehaviorSummary> summarizeTargets(
            List<BehaviorEventEntity> events,
            Function<BehaviorEventEntity, Long> targetExtractor,
            String targetType
    ) {
        Map<Long, List<BehaviorEventEntity>> groupedEvents = events.stream()
                .filter(event -> targetExtractor.apply(event) != null)
                .collect(Collectors.groupingBy(
                        targetExtractor,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return groupedEvents.entrySet()
                .stream()
                .map(entry -> new AgentBehaviorSummary(
                        targetType,
                        entry.getKey(),
                        entry.getValue().size(),
                        latestOccurredAt(entry.getValue())
                ))
                .sorted(Comparator
                        .comparingLong(AgentBehaviorSummary::count)
                        .reversed()
                        .thenComparing(
                                AgentBehaviorSummary::lastOccurredAt,
                                Comparator.nullsLast(Comparator.reverseOrder())
                        ))
                .limit(MAX_SUMMARY_ITEMS)
                .toList();
    }

    private LocalDateTime latestOccurredAt(List<BehaviorEventEntity> events) {
        return events.stream()
                .map(BehaviorEventEntity::getOccurredAt)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }
}
