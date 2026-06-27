package com.personaflow.commerce.user.api.model;

import java.util.Set;

public record CurrentUser(
        Long userId,
        Set<String> roles
) {
}
