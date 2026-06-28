package com.personaflow.commerce.product.vo;

import java.math.BigDecimal;
import java.util.Map;

public record SkuDetailVO(
        Long skuId,
        Long spuId,
        String productName,
        String skuName,
        Long categoryId,
        String categoryName,
        BigDecimal price,
        BigDecimal originalPrice,
        String imageUrl,
        Map<String, Object> specs,
        Integer status
) {
}
