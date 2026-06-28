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
import com.personaflow.commerce.order.vo.OrderStatusVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

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
        OrderControllerCancelTest.TestConfig.class
})
@EnableConfigurationProperties(JwtProperties.class)
@TestPropertySource(properties = {
        "commerce.jwt.secret=test-jwt-secret-with-at-least-thirty-two-characters",
        "commerce.jwt.expires-in=7200"
})
class OrderControllerCancelTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        reset(orderService);
    }

    @Test
    void authenticatedUserCanCancelOrder() throws Exception {
        when(orderService.cancelOrder(50001L)).thenReturn(new OrderStatusVO(
                50001L,
                "PF20260628230000000123",
                30,
                LocalDateTime.of(2026, 6, 29, 0, 30)
        ));

        mockMvc.perform(post("/api/trade/orders/50001/cancel")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.orderId").value(50001))
                .andExpect(jsonPath("$.data.orderNo").value("PF20260628230000000123"))
                .andExpect(jsonPath("$.data.status").value(30))
                .andExpect(jsonPath("$.data.canceledAt").exists());

        verify(orderService).cancelOrder(50001L);
    }

    @Test
    void anonymousUserCannotCancelOrder() throws Exception {
        mockMvc.perform(post("/api/trade/orders/50001/cancel"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_UNAUTHORIZED"));

        verifyNoInteractions(orderService);
    }

    @Test
    void missingOrderReturnsNotFound() throws Exception {
        when(orderService.cancelOrder(99999L))
                .thenThrow(new BusinessException(ErrorCode.TRADE_ORDER_NOT_FOUND));

        mockMvc.perform(post("/api/trade/orders/99999/cancel")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.errorCode").value("TRADE_ORDER_NOT_FOUND"));

        verify(orderService).cancelOrder(99999L);
    }

    @Test
    void statusNotAllowedReturnsConflict() throws Exception {
        when(orderService.cancelOrder(50001L))
                .thenThrow(new BusinessException(ErrorCode.TRADE_ORDER_STATUS_NOT_ALLOWED));

        mockMvc.perform(post("/api/trade/orders/50001/cancel")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.errorCode").value("TRADE_ORDER_STATUS_NOT_ALLOWED"));

        verify(orderService).cancelOrder(50001L);
    }

    private String bearerToken() {
        return "Bearer " + jwtService.generateAccessToken(10001L, Set.of("ROLE_USER"));
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        OrderService orderService() {
            return mock(OrderService.class);
        }

        @Bean
        JwtService jwtService(JwtProperties properties) {
            return new JwtService(properties);
        }
    }
}
