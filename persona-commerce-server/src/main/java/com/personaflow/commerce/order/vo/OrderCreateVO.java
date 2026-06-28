package com.personaflow.commerce.order.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderCreateVO(
        Long orderId,
        String orderNo,
        BigDecimal totalAmount,
        Integer status,
        LocalDateTime createdAt,
        List<OrderItemVO> items
) {
}
