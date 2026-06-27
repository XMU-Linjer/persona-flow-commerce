package com.personaflow.commerce.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank
        @Size(min = 4, max = 32)
        @Pattern(regexp = "^[A-Za-z0-9_]+$")
        String username,

        @NotBlank
        @Size(min = 8, max = 64)
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$")
        String password,

        @NotBlank
        @Size(min = 1, max = 50)
        String displayName
) {
}
