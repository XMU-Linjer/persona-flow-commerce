package com.personaflow.commerce.payment.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentVO(
        String paymentNo,
        Long orderId,
        String orderNo,
        BigDecimal amount,
        String channel,
        Integer status,
        LocalDateTime paidAt
) {
}
