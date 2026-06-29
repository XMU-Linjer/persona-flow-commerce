package com.personaflow.commerce.behavior.controller;

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
import com.personaflow.commerce.common.api.ApiResponse;
import com.personaflow.commerce.user.api.CurrentUserProvider;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/behavior/me")
public class BehaviorController {

    private final CurrentUserProvider currentUserProvider;
    private final BehaviorQueryService behaviorQueryService;
    private final BehaviorSummaryService behaviorSummaryService;
    private final BehaviorContextService behaviorContextService;
    private final UserProfileVersionService userProfileVersionService;
    private final BehaviorProfileRefreshService behaviorProfileRefreshService;

    public BehaviorController(
            CurrentUserProvider currentUserProvider,
            BehaviorQueryService behaviorQueryService,
            BehaviorSummaryService behaviorSummaryService,
            BehaviorContextService behaviorContextService,
            UserProfileVersionService userProfileVersionService,
            BehaviorProfileRefreshService behaviorProfileRefreshService
    ) {
        this.currentUserProvider = currentUserProvider;
        this.behaviorQueryService = behaviorQueryService;
        this.behaviorSummaryService = behaviorSummaryService;
        this.behaviorContextService = behaviorContextService;
        this.userProfileVersionService = userProfileVersionService;
        this.behaviorProfileRefreshService = behaviorProfileRefreshService;
    }

    @GetMapping("/events")
    public ApiResponse<List<BehaviorEventVO>> listRecentEvents(
            @RequestParam(required = false) BehaviorEventType eventType,
            @Min(1) @RequestParam(defaultValue = "50") Integer limit
    ) {
        Long userId = currentUserProvider.requireCurrentUser().userId();
        return ApiResponse.success(behaviorQueryService.findRecentEvents(userId, eventType, limit));
    }

    @GetMapping("/summary")
    public ApiResponse<BehaviorSummaryVO> getSummary(
            @Min(1) @RequestParam(defaultValue = "30") Integer days
    ) {
        Long userId = currentUserProvider.requireCurrentUser().userId();
        return ApiResponse.success(behaviorSummaryService.summarize(userId, days));
    }

    @GetMapping("/agent-context")
    public ApiResponse<AgentProfileContext> getAgentContext(
            @Min(1) @RequestParam(defaultValue = "30") Integer days
    ) {
        Long userId = currentUserProvider.requireCurrentUser().userId();
        return ApiResponse.success(behaviorContextService.buildAgentProfileContext(userId, days));
    }

    @GetMapping("/profile/latest")
    public ApiResponse<UserProfileLatestVO> getLatestProfile() {
        Long userId = currentUserProvider.requireCurrentUser().userId();
        return ApiResponse.success(UserProfileLatestVO.from(userId, userProfileVersionService.findLatestProfile(userId)));
    }

    @PostMapping("/profile/refresh")
    public ApiResponse<UserProfileLatestVO> refreshProfile(
            @Min(1) @RequestParam(defaultValue = "30") Integer days
    ) {
        return ApiResponse.success(behaviorProfileRefreshService.refreshCurrentUserProfile(days));
    }
}
