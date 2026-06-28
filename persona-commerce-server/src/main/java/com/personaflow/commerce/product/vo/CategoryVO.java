package com.personaflow.commerce.product.vo;

import java.util.List;

public record CategoryVO(
        Long id,
        String name,
        Long parentId,
        Integer level,
        Integer sortOrder,
        String iconUrl,
        List<CategoryVO> children
) {
}
