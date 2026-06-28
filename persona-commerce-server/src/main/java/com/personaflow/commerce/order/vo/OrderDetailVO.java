package com.personaflow.commerce.order.vo;

import com.personaflow.commerce.payment.vo.PaymentVO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderDetailVO(
        Long orderId,
        String orderNo,
        Long userId,
        OrderAddressVO address,
        BigDecimal totalAmount,
        Integer status,
        LocalDateTime createdAt,
        LocalDateTime paidAt,
        LocalDateTime canceledAt,
        List<OrderItemVO> items,
        PaymentVO payment
) {
}
