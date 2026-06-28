package com.personaflow.commerce.payment.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentVO(
        String paymentNo,
        BigDecimal amount,
        String channel,
        Integer status,
        LocalDateTime paidAt
) {
}
