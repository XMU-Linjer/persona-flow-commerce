package com.personaflow.commerce.order.controller;

import com.personaflow.commerce.auth.security.JwtAuthenticationFilter;
import com.personaflow.commerce.auth.security.JwtProperties;
import com.personaflow.commerce.auth.security.JwtService;
import com.personaflow.commerce.auth.security.SecurityConfig;
import com.personaflow.commerce.common.error.BusinessException;
import com.personaflow.commerce.common.error.ErrorCode;
import com.personaflow.commerce.common.error.GlobalExceptionHandler;
import com.personaflow.commerce.common.error.RestAccessDeniedHandler;
import com.personaflow.commerce.common.error.RestAuthenticationEntryPoint;
import com.personaflow.commerce.common.error.SecurityErrorResponseWriter;
import com.personaflow.commerce.order.service.OrderService;
import com.personaflow.commerce.payment.dto.PayOrderRequest;
import com.personaflow.commerce.payment.service.PaymentService;
import com.personaflow.commerce.payment.vo.PaymentVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OrderController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class,
        SecurityErrorResponseWriter.class,
        GlobalExceptionHandler.class,
        OrderControllerPayTest.TestConfig.class
})
@EnableConfigurationProperties(JwtProperties.class)
@TestPropertySource(properties = {
        "commerce.jwt.secret=test-jwt-secret-with-at-least-thirty-two-characters",
        "commerce.jwt.expires-in=7200"
})
class OrderControllerPayTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        reset(orderService, paymentService);
    }

    @Test
    void authenticatedUserCanPayOrder() throws Exception {
        PayOrderRequest request = new PayOrderRequest("MOCK");
        when(paymentService.payOrder(50001L, request)).thenReturn(paymentVO());

        mockMvc.perform(post("/api/trade/orders/50001/pay")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "channel": "MOCK"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.paymentNo").value("PAY20260629093000123456"))
                .andExpect(jsonPath("$.data.orderId").value(50001))
                .andExpect(jsonPath("$.data.orderNo").value("PF20260628230000000123"))
                .andExpect(jsonPath("$.data.amount").value(918.00))
                .andExpect(jsonPath("$.data.channel").value("MOCK"))
                .andExpect(jsonPath("$.data.status").value(20))
                .andExpect(jsonPath("$.data.paidAt").exists());

        verify(paymentService).payOrder(50001L, request);
        verifyNoInteractions(orderService);
    }

    @Test
    void anonymousUserCannotPayOrder() throws Exception {
        mockMvc.perform(post("/api/trade/orders/50001/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "channel": "MOCK"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_UNAUTHORIZED"));

        verifyNoInteractions(orderService, paymentService);
    }

    @Test
    void missingOrderReturnsNotFound() throws Exception {
        PayOrderRequest request = new PayOrderRequest("MOCK");
        when(paymentService.payOrder(99999L, request))
                .thenThrow(new BusinessException(ErrorCode.TRADE_ORDER_NOT_FOUND));

        mockMvc.perform(post("/api/trade/orders/99999/pay")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "channel": "MOCK"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.errorCode").value("TRADE_ORDER_NOT_FOUND"));

        verify(paymentService).payOrder(99999L, request);
        verifyNoInteractions(orderService);
    }

    @Test
    void statusNotAllowedReturnsConflict() throws Exception {
        PayOrderRequest request = new PayOrderRequest("MOCK");
        when(paymentService.payOrder(50001L, request))
                .thenThrow(new BusinessException(ErrorCode.TRADE_ORDER_STATUS_NOT_ALLOWED));

        mockMvc.perform(post("/api/trade/orders/50001/pay")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "channel": "MOCK"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.errorCode").value("TRADE_ORDER_STATUS_NOT_ALLOWED"));

        verify(paymentService).payOrder(50001L, request);
        verifyNoInteractions(orderService);
    }

    private String bearerToken() {
        return "Bearer " + jwtService.generateAccessToken(10001L, Set.of("ROLE_USER"));
    }

    private PaymentVO paymentVO() {
        return new PaymentVO(
                "PAY20260629093000123456",
                50001L,
                "PF20260628230000000123",
                new BigDecimal("918.00"),
                "MOCK",
                20,
                LocalDateTime.of(2026, 6, 29, 9, 30)
        );
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        OrderService orderService() {
            return mock(OrderService.class);
        }

        @Bean
        PaymentService paymentService() {
            return mock(PaymentService.class);
        }

        @Bean
        JwtService jwtService(JwtProperties properties) {
            return new JwtService(properties);
        }
    }
}
