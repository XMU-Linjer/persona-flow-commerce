package com.personaflow.commerce.user.vo;

import java.util.Set;

public record CurrentUserVO(
        Long userId,
        String username,
        String displayName,
        String avatarUrl,
        Set<String> roles
) {
}
