package com.personaflow.commerce.cart.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddCartItemRequest(
        @NotNull
        @Positive
        Long skuId,

        @NotNull
        Integer quantity
) {
}
