package com.personaflow.commerce.behavior.controller;

import com.personaflow.commerce.auth.security.JwtAuthenticationFilter;
import com.personaflow.commerce.auth.security.JwtProperties;
import com.personaflow.commerce.auth.security.JwtService;
import com.personaflow.commerce.auth.security.SecurityConfig;
import com.personaflow.commerce.behavior.enums.BehaviorEventType;
import com.personaflow.commerce.behavior.service.BehaviorContextService;
import com.personaflow.commerce.behavior.service.BehaviorProfileRefreshService;
import com.personaflow.commerce.behavior.service.BehaviorQueryService;
import com.personaflow.commerce.behavior.service.BehaviorSummaryService;
import com.personaflow.commerce.behavior.service.UserProfileVersionService;
import com.personaflow.commerce.behavior.vo.AgentProfileContext;
import com.personaflow.commerce.behavior.vo.BehaviorEventVO;
import com.personaflow.commerce.behavior.vo.BehaviorSummaryVO;
import com.personaflow.commerce.behavior.vo.UserProfileLatestVO;
import com.personaflow.commerce.common.error.GlobalExceptionHandler;
import com.personaflow.commerce.common.error.RestAccessDeniedHandler;
import com.personaflow.commerce.common.error.RestAuthenticationEntryPoint;
import com.personaflow.commerce.common.error.SecurityErrorResponseWriter;
import com.personaflow.commerce.user.api.CurrentUserProvider;
import com.personaflow.commerce.user.api.model.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BehaviorController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class,
        SecurityErrorResponseWriter.class,
        GlobalExceptionHandler.class,
        BehaviorControllerTest.TestConfig.class
})
@EnableConfigurationProperties(JwtProperties.class)
@TestPropertySource(properties = {
        "commerce.jwt.secret=test-jwt-secret-with-at-least-thirty-two-characters",
        "commerce.jwt.expires-in=7200"
})
class BehaviorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CurrentUserProvider currentUserProvider;

    @Autowired
    private BehaviorQueryService behaviorQueryService;

    @Autowired
    private BehaviorSummaryService behaviorSummaryService;

    @Autowired
    private BehaviorContextService behaviorContextService;

    @Autowired
    private UserProfileVersionService userProfileVersionService;

    @Autowired
    private BehaviorProfileRefreshService behaviorProfileRefreshService;

    @BeforeEach
    void setUp() {
        reset(
                currentUserProvider,
                behaviorQueryService,
                behaviorSummaryService,
                behaviorContextService,
                userProfileVersionService,
                behaviorProfileRefreshService
        );
    }

    @Test
    void authenticatedUserCanQueryRecentEventsWithEventTypeFilter() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser());
        when(behaviorQueryService.findRecentEvents(
                eq(10001L),
                eq(BehaviorEventType.PRODUCT_VIEW),
                eq(500)
        )).thenReturn(List.of(behaviorEventVO()));

        mockMvc.perform(get("/api/behavior/me/events")
                        .header("Authorization", bearerToken())
                        .param("eventType", "PRODUCT_VIEW")
                        .param("limit", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].eventId").value("event-view"))
                .andExpect(jsonPath("$.data[0].eventType").value("PRODUCT_VIEW"))
                .andExpect(jsonPath("$.data[0].skuId").value(30001));

        verify(behaviorQueryService).findRecentEvents(10001L, BehaviorEventType.PRODUCT_VIEW, 500);
    }

    @Test
    void authenticatedUserCanQuerySummary() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser());
        when(behaviorSummaryService.summarize(10001L, 30)).thenReturn(new BehaviorSummaryVO(
                10001L,
                Map.of("PRODUCT_VIEW", 2L),
                List.of("keyboard"),
                List.of(),
                List.of(),
                List.of(),
                LocalDateTime.of(2026, 6, 29, 12, 0)
        ));

        mockMvc.perform(get("/api/behavior/me/summary")
                        .header("Authorization", bearerToken())
                        .param("days", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userId").value(10001))
                .andExpect(jsonPath("$.data.eventTypeCounts.PRODUCT_VIEW").value(2))
                .andExpect(jsonPath("$.data.recentKeywords[0]").value("keyboard"));

        verify(behaviorSummaryService).summarize(10001L, 30);
    }

    @Test
    void authenticatedUserCanQueryAgentContext() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser());
        when(behaviorContextService.buildAgentProfileContext(10001L, 30)).thenReturn(agentProfileContext());

        mockMvc.perform(get("/api/behavior/me/agent-context")
                        .header("Authorization", bearerToken())
                        .param("days", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userId").value(10001))
                .andExpect(jsonPath("$.data.eventTypeCounts.PAYMENT_SUCCESS").value(1))
                .andExpect(jsonPath("$.data.fulfilledNeeds[0].fulfilled").value(true))
                .andExpect(jsonPath("$.data.fulfilledNeeds[0].complementTrigger").value(true))
                .andExpect(jsonPath("$.data.fulfilledNeeds[0].repeatRecommendationSuppressed").value(true));

        verify(behaviorContextService).buildAgentProfileContext(10001L, 30);
    }

    @Test
    void profileLatestReturnsEmptyResultWhenNoProfileExists() throws Exception {
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser());
        when(userProfileVersionService.findLatestProfile(10001L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/behavior/me/profile/latest")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userId").value(10001))
                .andExpect(jsonPath("$.data.exists").value(false))
                .andExpect(jsonPath("$.data.profileVersionId").doesNotExist());

        verify(userProfileVersionService).findLatestProfile(10001L);
    }

    @Test
    void authenticatedUserCanRefreshProfile() throws Exception {
        when(behaviorProfileRefreshService.refreshCurrentUserProfile(30)).thenReturn(profileLatestVO());

        mockMvc.perform(post("/api/behavior/me/profile/refresh")
                        .header("Authorization", bearerToken())
                        .param("days", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userId").value(10001))
                .andExpect(jsonPath("$.data.exists").value(true))
                .andExpect(jsonPath("$.data.sourceWorkflowId").value("workflow-001"));

        verify(behaviorProfileRefreshService).refreshCurrentUserProfile(30);
    }

    @Test
    void anonymousUserCannotAccessBehaviorEndpoints() throws Exception {
        mockMvc.perform(get("/api/behavior/me/events"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_UNAUTHORIZED"));

        mockMvc.perform(get("/api/behavior/me/summary"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_UNAUTHORIZED"));

        mockMvc.perform(get("/api/behavior/me/agent-context"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_UNAUTHORIZED"));

        mockMvc.perform(get("/api/behavior/me/profile/latest"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_UNAUTHORIZED"));

        mockMvc.perform(post("/api/behavior/me/profile/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_UNAUTHORIZED"));

        verifyNoInteractions(
                currentUserProvider,
                behaviorQueryService,
                behaviorSummaryService,
                behaviorContextService,
                userProfileVersionService,
                behaviorProfileRefreshService
        );
    }

    private String bearerToken() {
        return "Bearer " + jwtService.generateAccessToken(10001L, Set.of("ROLE_USER"));
    }

    private CurrentUser currentUser() {
        return new CurrentUser(10001L, Set.of("ROLE_USER"));
    }

    private BehaviorEventVO behaviorEventVO() {
        return new BehaviorEventVO(
                1L,
                "event-view",
                "PRODUCT_VIEW",
                "catalog",
                "SKU",
                30001L,
                null,
                30001L,
                20001L,
                201L,
                null,
                null,
                "{}",
                LocalDateTime.of(2026, 6, 29, 10, 0)
        );
    }

    private AgentProfileContext agentProfileContext() {
        return new AgentProfileContext(
                10001L,
                List.of(),
                Map.of("PAYMENT_SUCCESS", 1L),
                List.of("keyboard"),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(new com.personaflow.commerce.behavior.vo.AgentDemandSignal(
                        "event-paid",
                        "PAYMENT_SUCCESS",
                        30001L,
                        20001L,
                        201L,
                        50001L,
                        new BigDecimal("918.00"),
                        true,
                        true,
                        true,
                        true,
                        LocalDateTime.of(2026, 6, 29, 10, 0)
                )),
                List.of(),
                List.of(new com.personaflow.commerce.behavior.vo.AgentDemandSignal(
                        "event-paid",
                        "PAYMENT_SUCCESS",
                        30001L,
                        20001L,
                        201L,
                        50001L,
                        new BigDecimal("918.00"),
                        true,
                        true,
                        true,
                        true,
                        LocalDateTime.of(2026, 6, 29, 10, 0)
                )),
                List.of("event-paid"),
                List.of(),
                LocalDateTime.of(2026, 6, 29, 12, 0)
        );
    }

    private UserProfileLatestVO profileLatestVO() {
        return new UserProfileLatestVO(
                10001L,
                true,
                1L,
                1782748800,
                "{\"summary\":\"profile\"}",
                "profile",
                "workflow-001",
                LocalDateTime.of(2026, 6, 29, 12, 0)
        );
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        CurrentUserProvider currentUserProvider() {
            return mock(CurrentUserProvider.class);
        }

        @Bean
        BehaviorQueryService behaviorQueryService() {
            return mock(BehaviorQueryService.class);
        }

        @Bean
        BehaviorSummaryService behaviorSummaryService() {
            return mock(BehaviorSummaryService.class);
        }

        @Bean
        BehaviorContextService behaviorContextService() {
            return mock(BehaviorContextService.class);
        }

        @Bean
        UserProfileVersionService userProfileVersionService() {
            return mock(UserProfileVersionService.class);
        }

        @Bean
        BehaviorProfileRefreshService behaviorProfileRefreshService() {
            return mock(BehaviorProfileRefreshService.class);
        }

        @Bean
        JwtService jwtService(JwtProperties properties) {
            return new JwtService(properties);
        }
    }
}
