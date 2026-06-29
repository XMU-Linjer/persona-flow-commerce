package com.personaflow.commerce.behavior.vo;

import com.personaflow.commerce.behavior.entity.UserProfileVersionEntity;

import java.time.LocalDateTime;
import java.util.Optional;

public record UserProfileLatestVO(
        Long userId,
        boolean exists,
        Long profileVersionId,
        Integer versionNo,
        String profileJson,
        String summary,
        String sourceWorkflowId,
        LocalDateTime createdAt
) {

    public static UserProfileLatestVO empty(Long userId) {
        return new UserProfileLatestVO(userId, false, null, null, null, null, null, null);
    }

    public static UserProfileLatestVO from(Long userId, Optional<UserProfileVersionEntity> profileVersion) {
        return profileVersion
                .map(profile -> new UserProfileLatestVO(
                        userId,
                        true,
                        profile.getId(),
                        profile.getVersionNo(),
                        profile.getProfileJson(),
                        profile.getSummary(),
                        profile.getSourceWorkflowId(),
                        profile.getCreatedAt()
                ))
                .orElseGet(() -> empty(userId));
    }
}
