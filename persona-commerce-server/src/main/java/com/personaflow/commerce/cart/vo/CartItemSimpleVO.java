package com.personaflow.commerce.cart.vo;

public record CartItemSimpleVO(
        Long cartItemId,
        Long skuId,
        Integer quantity
) {
}
