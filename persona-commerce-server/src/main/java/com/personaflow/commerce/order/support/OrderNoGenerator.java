package com.personaflow.commerce.order.support;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class OrderNoGenerator {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final SecureRandom RANDOM = new SecureRandom();

    public String generate() {
        int suffix = RANDOM.nextInt(1_000_000);
        return "PF" + LocalDateTime.now().format(FORMATTER) + String.format("%06d", suffix);
    }
}
