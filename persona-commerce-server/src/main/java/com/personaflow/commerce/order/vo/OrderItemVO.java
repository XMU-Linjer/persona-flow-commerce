package com.personaflow.commerce.order.vo;

import java.math.BigDecimal;

public record OrderItemVO(
        Long skuId,
        Long spuId,
        Long categoryId,
        String categoryName,
        String productName,
        String skuName,
        String imageUrl,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal subtotal
) {
}
