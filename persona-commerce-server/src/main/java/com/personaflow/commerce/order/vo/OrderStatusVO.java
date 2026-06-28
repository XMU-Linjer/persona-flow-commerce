package com.personaflow.commerce.order.vo;

import java.time.LocalDateTime;

public record OrderStatusVO(
        Long orderId,
        String orderNo,
        Integer status,
        LocalDateTime canceledAt
) {
}
