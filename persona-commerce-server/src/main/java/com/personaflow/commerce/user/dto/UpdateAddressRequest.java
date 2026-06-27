package com.personaflow.commerce.user.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UpdateAddressRequest(
        @NotBlank
        @Size(max = 50)
        String recipientName,

        @NotBlank
        @Size(max = 30)
        String recipientPhone,

        @NotBlank
        @Size(max = 50)
        String province,

        @NotBlank
        @Size(max = 50)
        String city,

        @NotBlank
        @Size(max = 50)
        String district,

        @NotBlank
        @Size(max = 255)
        String detailAddress,

        @Size(max = 20)
        String postalCode,

        Boolean isDefault
) {
}
