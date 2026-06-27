package com.personaflow.commerce.auth.security;

import java.util.Set;

public record AuthenticatedUser(
        Long userId,
        Set<String> roles
) {
}
