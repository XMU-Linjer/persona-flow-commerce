package com.personaflow.commerce.favorite.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FavoriteItemVO(
        Long favoriteId,
        Long skuId,
        Long spuId,
        Long categoryId,
        String categoryName,
        String productName,
        String skuName,
        BigDecimal unitPrice,
        String imageUrl,
        LocalDateTime createdAt
) {
}
