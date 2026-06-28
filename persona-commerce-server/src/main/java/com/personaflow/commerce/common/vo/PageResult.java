package com.personaflow.commerce.common.vo;

import java.util.List;

public record PageResult<T>(
        List<T> records,
        int page,
        int size,
        long total
) {
}
