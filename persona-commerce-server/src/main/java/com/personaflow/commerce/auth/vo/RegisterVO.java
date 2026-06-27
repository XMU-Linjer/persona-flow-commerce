package com.personaflow.commerce.auth.vo;

public record RegisterVO(
        Long userId,
        String username,
        String displayName
) {
}
