package com.personaflow.commerce.auth.vo;

import java.util.Set;

public record LoginVO(
        String accessToken,
        String tokenType,
        long expiresIn,
        LoginUserVO user
) {

    public record LoginUserVO(
            Long id,
            String username,
            String displayName,
            Set<String> roles
    ) {
    }
}
