package com.personaflow.commerce.behavior.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Hybrid trigger for automatic profile refresh:
 *
 * <ol>
 *   <li><b>Count threshold (N events)</b>: prevents one-event-one-refresh thundering herd.</li>
 *   <li><b>Max delay (T seconds)</b>: guarantees the last partial batch is flushed.</li>
 *   <li><b>Idle suppression</b>: no timer runs when there are zero unprocessed events — zero waste.</li>
 * </ol>
 *
 * Integration point: {@code BehaviorEventConsumer} calls {@link #onNewEvent(Long)}
 * after a behavior event is successfully persisted.
 */
@Service
public class ProfileRefreshScheduler {

    private static final Logger log = LoggerFactory.getLogger(ProfileRefreshScheduler.class);

    private final BehaviorProfileRefreshService refreshService;
    private final ScheduledExecutorService scheduler;

    private final ConcurrentHashMap<Long, PendingRefresh> pending = new ConcurrentHashMap<>();

    @Value("${behavior.profile.refresh.batch-threshold:5}")
    private int batchThreshold;

    @Value("${behavior.profile.refresh.max-delay-seconds:30}")
    private int maxDelaySeconds;

    @Value("${behavior.profile.refresh.days:30}")
    private int profileDays;

    @Value("${behavior.profile.refresh.enabled:true}")
    private boolean enabled;

    @Value("${behavior.profile.refresh.stale-timeout-seconds:300}")
    private int staleTimeoutSeconds;

    public ProfileRefreshScheduler(BehaviorProfileRefreshService refreshService) {
        this.refreshService = refreshService;
        this.scheduler = Executors.newScheduledThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable, "profile-refresh-scheduler");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Called by the behavior event consumer after a new event is persisted.
     * Non-blocking — only updates counters and schedules async tasks.
     */
    public void onNewEvent(Long userId) {
        if (!enabled || userId == null) {
            return;
        }

        pending.compute(userId, (id, state) -> {
            if (state == null) {
                ScheduledFuture<?> deferred = scheduler.schedule(
                        () -> flushAsync(userId),
                        maxDelaySeconds,
                        TimeUnit.SECONDS
                );
                return new PendingRefresh(Instant.now(), new AtomicInteger(1), deferred);
            }

            int count = state.unrefreshedCount.incrementAndGet();
            if (count >= batchThreshold) {
                state.deferredTask.cancel(false);
                scheduler.execute(() -> flushAsync(userId));
                return null;
            }
            return state;
        });
    }

    /**
     * Periodically remove zombie entries (e.g., timer was cancelled but
     * the entry was not cleaned up due to an unexpected error).
     */
    @Scheduled(fixedDelayString = "${behavior.profile.refresh.cleanup-interval-seconds:300}")
    public void cleanupStaleEntries() {
        Instant cutoff = Instant.now().minus(Duration.ofSeconds(staleTimeoutSeconds));
        pending.entrySet().removeIf(entry -> {
            if (entry.getValue().firstEventAt.isBefore(cutoff)) {
                log.warn("Removing stale pending refresh entry userId={}", entry.getKey());
                entry.getValue().deferredTask.cancel(false);
                return true;
            }
            return false;
        });
    }

    private void flushAsync(Long userId) {
        pending.remove(userId);
        try {
            refreshService.refreshByUserId(userId, profileDays);
            log.info("Auto profile refresh triggered userId={}", userId);
        } catch (Exception exception) {
            log.warn("Auto profile refresh failed userId={}, errorType={}, reason={}",
                    userId, exception.getClass().getSimpleName(), exception.getMessage());
        }
    }

    // ---------- visible for testing ----------

    int pendingCount() {
        return pending.size();
    }

    PendingRefresh pendingFor(Long userId) {
        return pending.get(userId);
    }

    void clearForTest() {
        pending.values().forEach(state -> state.deferredTask.cancel(false));
        pending.clear();
    }

    // ---------- inner record ----------

    static final class PendingRefresh {
        final Instant firstEventAt;
        final AtomicInteger unrefreshedCount;
        final ScheduledFuture<?> deferredTask;

        PendingRefresh(Instant firstEventAt, AtomicInteger unrefreshedCount, ScheduledFuture<?> deferredTask) {
            this.firstEventAt = firstEventAt;
            this.unrefreshedCount = unrefreshedCount;
            this.deferredTask = deferredTask;
        }
    }
}