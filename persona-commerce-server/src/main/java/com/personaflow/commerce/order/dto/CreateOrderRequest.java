package com.personaflow.commerce.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record CreateOrderRequest(
        @NotNull
        @Positive
        Long addressId,

        @NotNull
        List<@Valid CreateOrderItemRequest> items
) {
}
