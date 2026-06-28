package com.personaflow.commerce.product.vo;

import java.math.BigDecimal;
import java.util.Map;

public record SkuVO(
        Long skuId,
        String skuName,
        Map<String, Object> specs,
        BigDecimal price,
        BigDecimal originalPrice,
        String imageUrl,
        Integer status,
        Integer salesCount
) {
}
