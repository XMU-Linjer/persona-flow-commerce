package com.personaflow.commerce.cart.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CartItemVO(
        Long cartItemId,
        Long skuId,
        Long spuId,
        Long categoryId,
        String categoryName,
        String productName,
        String skuName,
        BigDecimal unitPrice,
        String imageUrl,
        Integer quantity,
        BigDecimal subtotal,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
