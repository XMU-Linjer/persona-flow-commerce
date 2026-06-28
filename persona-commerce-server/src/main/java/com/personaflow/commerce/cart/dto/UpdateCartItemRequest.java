package com.personaflow.commerce.cart.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateCartItemRequest(
        @NotNull
        Integer quantity
) {
}
