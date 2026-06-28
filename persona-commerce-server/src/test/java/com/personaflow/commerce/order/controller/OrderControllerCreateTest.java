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
import com.personaflow.commerce.order.dto.CreateOrderItemRequest;
import com.personaflow.commerce.order.dto.CreateOrderRequest;
import com.personaflow.commerce.order.service.OrderService;
import com.personaflow.commerce.order.vo.OrderCreateVO;
import com.personaflow.commerce.order.vo.OrderItemVO;
import com.personaflow.commerce.payment.service.PaymentService;
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
import java.util.List;
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
        OrderControllerCreateTest.TestConfig.class
})
@EnableConfigurationProperties(JwtProperties.class)
@TestPropertySource(properties = {
        "commerce.jwt.secret=test-jwt-secret-with-at-least-thirty-two-characters",
        "commerce.jwt.expires-in=7200"
})
class OrderControllerCreateTest {

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
    void authenticatedUserCanCreateOrder() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                1L,
                List.of(new CreateOrderItemRequest(30001L, 2))
        );
        when(orderService.createOrder(request)).thenReturn(orderCreateVO());

        mockMvc.perform(post("/api/trade/orders")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "addressId": 1,
                                  "items": [
                                    {
                                      "skuId": 30001,
                                      "quantity": 2
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.orderId").value(50001))
                .andExpect(jsonPath("$.data.orderNo").value("PF20260628230000000123"))
                .andExpect(jsonPath("$.data.totalAmount").value(918.00))
                .andExpect(jsonPath("$.data.status").value(10))
                .andExpect(jsonPath("$.data.items[0].skuId").value(30001))
                .andExpect(jsonPath("$.data.items[0].productName").value("KeyForge K3"))
                .andExpect(jsonPath("$.data.items[0].subtotal").value(918.00));

        verify(orderService).createOrder(request);
    }

    @Test
    void anonymousUserCannotCreateOrder() throws Exception {
        mockMvc.perform(post("/api/trade/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "addressId": 1,
                                  "items": [
                                    {
                                      "skuId": 30001,
                                      "quantity": 2
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_UNAUTHORIZED"));

        verifyNoInteractions(orderService);
    }

    @Test
    void invalidOrderItemsReturnTradeError() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(1L, List.of());
        when(orderService.createOrder(request))
                .thenThrow(new BusinessException(ErrorCode.TRADE_ORDER_EMPTY_ITEMS));

        mockMvc.perform(post("/api/trade/orders")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "addressId": 1,
                                  "items": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.errorCode").value("TRADE_ORDER_EMPTY_ITEMS"));

        verify(orderService).createOrder(request);
    }

    private String bearerToken() {
        return "Bearer " + jwtService.generateAccessToken(10001L, Set.of("ROLE_USER"));
    }

    private OrderCreateVO orderCreateVO() {
        return new OrderCreateVO(
                50001L,
                "PF20260628230000000123",
                new BigDecimal("918.00"),
                10,
                LocalDateTime.of(2026, 6, 28, 23, 0),
                List.of(new OrderItemVO(
                        30001L,
                        20001L,
                        201L,
                        "键盘鼠标",
                        "KeyForge K3",
                        "青轴 白色",
                        "sku-30001.jpg",
                        new BigDecimal("459.00"),
                        2,
                        new BigDecimal("918.00")
                ))
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
