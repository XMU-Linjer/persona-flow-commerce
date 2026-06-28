package com.personaflow.commerce.order.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderListItemVO(
        Long orderId,
        String orderNo,
        BigDecimal totalAmount,
        Integer status,
        LocalDateTime createdAt,
        LocalDateTime paidAt,
        LocalDateTime canceledAt
) {
}
