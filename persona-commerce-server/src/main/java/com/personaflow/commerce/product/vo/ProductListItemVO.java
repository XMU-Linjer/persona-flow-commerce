package com.personaflow.commerce.product.vo;

import java.math.BigDecimal;

public record ProductListItemVO(
        Long spuId,
        Long categoryId,
        String categoryName,
        String name,
        String subtitle,
        String brand,
        String mainImageUrl,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Integer salesCount,
        String tags,
        Integer status
) {
}
