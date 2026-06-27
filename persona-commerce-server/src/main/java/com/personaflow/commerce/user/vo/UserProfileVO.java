package com.personaflow.commerce.user.vo;

public record UserProfileVO(
        Long userId,
        String displayName,
        String avatarUrl
) {
}
