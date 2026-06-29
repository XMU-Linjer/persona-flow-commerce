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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class BehaviorProfileRefreshService {

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
        AgentProfileContext context = behaviorContextService.buildAgentProfileContext(userId, days);
        AgentProfileBuildResponse response = agentProfileClient.buildProfile(context);
        AgentProfileResult profile = response.profile();
        validateProfileUser(userId, profile);

        UserProfileVersionEntity saved = userProfileVersionService.saveProfileVersion(
                new UserProfileVersionCreateCommand(
                        userId,
                        generatedVersionNo(),
                        profileJson(profile),
                        profile.summary(),
                        sourceWorkflowId(response, profile)
                )
        );
        return UserProfileLatestVO.from(userId, Optional.of(saved));
    }

    private static void validateProfileUser(Long userId, AgentProfileResult profile) {
        if (profile == null || profile.profile() == null) {
            throw new BusinessException(ErrorCode.AGENT_PROFILE_BUILD_FAILED);
        }
        if (profile.userId() != null && !profile.userId().equals(userId)) {
            throw new BusinessException(ErrorCode.AGENT_PROFILE_BUILD_FAILED);
        }
    }

    private static Integer generatedVersionNo() {
        return Math.toIntExact(Instant.now().getEpochSecond());
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
