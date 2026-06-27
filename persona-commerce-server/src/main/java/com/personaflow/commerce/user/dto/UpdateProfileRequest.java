package com.personaflow.commerce.user.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UpdateProfileRequest(
        @NotBlank
        @Size(min = 1, max = 50)
        String displayName,

        @Size(max = 500)
        String avatarUrl
) {
}
