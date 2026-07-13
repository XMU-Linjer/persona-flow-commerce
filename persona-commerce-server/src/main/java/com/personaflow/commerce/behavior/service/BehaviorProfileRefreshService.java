package com.personaflow.commerce.behavior.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.personaflow.commerce.behavior.agent.AgentProfileClient;
import com.personaflow.commerce.behavior.agent.dto.AgentProfileBuildResponse;
import com.personaflow.commerce.behavior.agent.dto.AgentProfileResult;
import com.personaflow.commerce.behavior.dto.UserProfileVersionCreateCommand;
import com.personaflow.commerce.behavior.entity.UserProfileVersionEntity;
import com.personaflow.commerce.behavior.vo.AgentProfileContext;
import com.personaflow.commerce.behavior.vo.UserProfileLatestVO;
import com.personaflow.commerce.common.error.BusinessException;
import com.personaflow.commerce.common.error.ErrorCode;
import com.personaflow.commerce.user.api.CurrentUserProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class BehaviorProfileRefreshService {

    private static final Logger log = LoggerFactory.getLogger(BehaviorProfileRefreshService.class);

    private final CurrentUserProvider currentUserProvider;
    private final BehaviorContextService behaviorContextService;
    private final AgentProfileClient agentProfileClient;
    private final UserProfileVersionService userProfileVersionService;
    private final ObjectMapper objectMapper;

    public BehaviorProfileRefreshService(
            CurrentUserProvider currentUserProvider,
            BehaviorContextService behaviorContextService,
            AgentProfileClient agentProfileClient,
            UserProfileVersionService userProfileVersionService,
            ObjectMapper objectMapper
    ) {
        this.currentUserProvider = currentUserProvider;
        this.behaviorContextService = behaviorContextService;
        this.agentProfileClient = agentProfileClient;
        this.userProfileVersionService = userProfileVersionService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public UserProfileLatestVO refreshCurrentUserProfile(Integer days) {
        Long userId = currentUserProvider.requireCurrentUser().userId();
        UserProfileVersionEntity saved = doRefresh(userId, days);
        return UserProfileLatestVO.from(userId, Optional.of(saved));
    }

    @Transactional
    public void refreshByUserId(Long userId, Integer days) {
        doRefresh(userId, days);
    }

    private UserProfileVersionEntity doRefresh(Long userId, Integer days) {
        long startedAt = System.nanoTime();
        log.info("Profile refresh started userId={}, days={}", userId, days);
        try {
            AgentProfileContext context = behaviorContextService.buildAgentProfileContext(userId, days);
            log.info(
                    "Profile context built userId={}, recentEvents={}, evidenceEvents={}, eventTypes={}",
                    userId,
                    sizeOf(context.recentEvents()),
                    sizeOf(context.evidenceEventIds()),
                    context.eventTypeCounts() == null ? 0 : context.eventTypeCounts().size()
            );
            AgentProfileBuildResponse response = agentProfileClient.buildProfile(context);
            AgentProfileResult profile = response.profile();
            validateProfileUser(userId, profile);

            UserProfileVersionEntity saved = userProfileVersionService.saveProfileVersion(
                    new UserProfileVersionCreateCommand(
                            userId,
                            null,
                            profileJson(profile),
                            profile.summary(),
                            sourceWorkflowId(response, profile)
                    )
            );
            log.info(
                    "Profile refresh succeeded userId={}, workflowId={}, profileVersionId={}, versionNo={}, elapsedMs={}",
                    userId,
                    saved.getSourceWorkflowId(),
                    saved.getId(),
                    saved.getVersionNo(),
                    elapsedMillis(startedAt)
            );
            return saved;
        } catch (RuntimeException exception) {
            log.warn(
                    "Profile refresh failed userId={}, days={}, elapsedMs={}, errorType={}, reason={}",
                    userId,
                    days,
                    elapsedMillis(startedAt),
                    exception.getClass().getSimpleName(),
                    exception.getMessage()
            );
            throw exception;
        }
    }

    private static int sizeOf(java.util.Collection<?> values) {
        return values == null ? 0 : values.size();
    }

    private static long elapsedMillis(long startedAt) {
        return java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }

    private static void validateProfileUser(Long userId, AgentProfileResult profile) {
        if (profile == null || profile.profile() == null) {
            throw new BusinessException(ErrorCode.AGENT_PROFILE_BUILD_FAILED);
        }
        if (profile.userId() != null && !profile.userId().equals(userId)) {
            throw new BusinessException(ErrorCode.AGENT_PROFILE_BUILD_FAILED);
        }
    }

    private String profileJson(AgentProfileResult profile) {
        try {
            return objectMapper.writeValueAsString(profile);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.AGENT_PROFILE_BUILD_FAILED);
        }
    }

    private static String sourceWorkflowId(AgentProfileBuildResponse response, AgentProfileResult profile) {
        if (response.workflowId() != null && !response.workflowId().isBlank()) {
            return response.workflowId();
        }
        return profile.workflowId();
    }
}
