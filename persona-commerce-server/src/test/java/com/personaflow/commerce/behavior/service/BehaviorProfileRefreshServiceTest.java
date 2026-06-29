package com.personaflow.commerce.behavior.service;

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
import com.personaflow.commerce.user.api.model.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BehaviorProfileRefreshServiceTest {

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private BehaviorContextService behaviorContextService;

    @Mock
    private AgentProfileClient agentProfileClient;

    @Mock
    private UserProfileVersionService userProfileVersionService;

    private BehaviorProfileRefreshService behaviorProfileRefreshService;

    @BeforeEach
    void setUp() {
        behaviorProfileRefreshService = new BehaviorProfileRefreshService(
                currentUserProvider,
                behaviorContextService,
                agentProfileClient,
                userProfileVersionService,
                new ObjectMapper()
        );
    }

    @Test
    void refreshCurrentUserProfileBuildsContextCallsAgentAndSavesVersion() {
        AgentProfileContext context = context(10001L);
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser(10001L));
        when(behaviorContextService.buildAgentProfileContext(10001L, 30)).thenReturn(context);
        when(agentProfileClient.buildProfile(context)).thenReturn(response(10001L));
        when(userProfileVersionService.saveProfileVersion(any())).thenAnswer(invocation -> {
            UserProfileVersionCreateCommand command = invocation.getArgument(0);
            return savedProfile(command);
        });

        long before = Instant.now().getEpochSecond();
        UserProfileLatestVO result = behaviorProfileRefreshService.refreshCurrentUserProfile(30);
        long after = Instant.now().getEpochSecond();

        assertThat(result.userId()).isEqualTo(10001L);
        assertThat(result.exists()).isTrue();
        assertThat(result.summary()).contains("keyboard");
        assertThat(result.sourceWorkflowId()).isEqualTo("workflow-001");

        ArgumentCaptor<UserProfileVersionCreateCommand> commandCaptor =
                ArgumentCaptor.forClass(UserProfileVersionCreateCommand.class);
        verify(userProfileVersionService).saveProfileVersion(commandCaptor.capture());
        UserProfileVersionCreateCommand command = commandCaptor.getValue();
        assertThat(command.userId()).isEqualTo(10001L);
        assertThat(command.versionNo()).isBetween((int) before, (int) after);
        assertThat(command.profileJson()).contains("\"artifactId\":\"profile-001\"");
        assertThat(command.profileJson()).contains("\"profileSummary\":\"Keyboard buyer\"");
        assertThat(command.summary()).contains("keyboard");
        assertThat(command.sourceWorkflowId()).isEqualTo("workflow-001");
    }

    @Test
    void refreshCurrentUserProfileUsesCurrentAuthenticatedUserOnly() {
        AgentProfileContext context = context(20002L);
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser(20002L));
        when(behaviorContextService.buildAgentProfileContext(20002L, 45)).thenReturn(context);
        when(agentProfileClient.buildProfile(context)).thenReturn(response(20002L));
        when(userProfileVersionService.saveProfileVersion(any())).thenAnswer(invocation -> {
            UserProfileVersionCreateCommand command = invocation.getArgument(0);
            return savedProfile(command);
        });

        behaviorProfileRefreshService.refreshCurrentUserProfile(45);

        verify(behaviorContextService).buildAgentProfileContext(20002L, 45);
        ArgumentCaptor<UserProfileVersionCreateCommand> commandCaptor =
                ArgumentCaptor.forClass(UserProfileVersionCreateCommand.class);
        verify(userProfileVersionService).saveProfileVersion(commandCaptor.capture());
        assertThat(commandCaptor.getValue().userId()).isEqualTo(20002L);
    }

    @Test
    void refreshCurrentUserProfileRejectsMismatchedAgentUser() {
        AgentProfileContext context = context(10001L);
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser(10001L));
        when(behaviorContextService.buildAgentProfileContext(10001L, 30)).thenReturn(context);
        when(agentProfileClient.buildProfile(context)).thenReturn(response(20002L));

        assertThatThrownBy(() -> behaviorProfileRefreshService.refreshCurrentUserProfile(30))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.AGENT_PROFILE_BUILD_FAILED));
        verifyNoInteractions(userProfileVersionService);
    }

    @Test
    void refreshCurrentUserProfileDoesNotSaveWhenAgentUnavailable() {
        AgentProfileContext context = context(10001L);
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser(10001L));
        when(behaviorContextService.buildAgentProfileContext(10001L, 30)).thenReturn(context);
        when(agentProfileClient.buildProfile(context)).thenThrow(new BusinessException(ErrorCode.AGENT_SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> behaviorProfileRefreshService.refreshCurrentUserProfile(30))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.AGENT_SERVICE_UNAVAILABLE));
        verifyNoInteractions(userProfileVersionService);
    }

    private CurrentUser currentUser(Long userId) {
        return new CurrentUser(userId, Set.of("ROLE_USER"));
    }

    private AgentProfileContext context(Long userId) {
        return new AgentProfileContext(
                userId,
                List.of(),
                Map.of("PAYMENT_SUCCESS", 1L),
                List.of("keyboard"),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of("event-paid"),
                List.of(),
                LocalDateTime.of(2026, 6, 29, 12, 0)
        );
    }

    private AgentProfileBuildResponse response(Long userId) {
        return new AgentProfileBuildResponse(
                "workflow-001",
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                new AgentProfileResult(
                        "profile-001",
                        "workflow-001",
                        userId,
                        "UserProfileVersion",
                        "2026-06-29T12:00:00Z",
                        new BigDecimal("0.82"),
                        List.of("event-paid"),
                        1,
                        "User recently bought a keyboard.",
                        Map.of(
                                "profileSummary", "Keyboard buyer",
                                "doNotRecommend", List.of(Map.of("skuId", 30001))
                        )
                )
        );
    }

    private UserProfileVersionEntity savedProfile(UserProfileVersionCreateCommand command) {
        UserProfileVersionEntity profile = new UserProfileVersionEntity();
        profile.setId(1L);
        profile.setUserId(command.userId());
        profile.setVersionNo(command.versionNo());
        profile.setProfileJson(command.profileJson());
        profile.setSummary(command.summary());
        profile.setSourceWorkflowId(command.sourceWorkflowId());
        profile.setCreatedAt(LocalDateTime.of(2026, 6, 29, 12, 0));
        return profile;
    }
}
