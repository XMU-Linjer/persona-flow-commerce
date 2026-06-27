package com.personaflow.commerce.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank
        String identityType,

        @NotBlank
        String identifier,

        @NotBlank
        String password
) {
}
