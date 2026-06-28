package com.personaflow.commerce.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateOrderItemRequest(
        @NotNull
        @Positive
        Long skuId,

        @NotNull
        Integer quantity
) {
}
