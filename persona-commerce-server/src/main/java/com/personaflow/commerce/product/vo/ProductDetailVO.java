package com.personaflow.commerce.product.vo;

import java.util.List;
import java.util.Map;

public record ProductDetailVO(
        Long spuId,
        Long categoryId,
        String categoryName,
        String name,
        String subtitle,
        String brand,
        String description,
        String mainImageUrl,
        List<String> detailImages,
        Map<String, Object> attributes,
        String tags,
        List<SkuVO> skus
) {
}
