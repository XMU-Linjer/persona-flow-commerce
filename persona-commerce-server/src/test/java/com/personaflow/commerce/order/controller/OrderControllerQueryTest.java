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
import com.personaflow.commerce.common.vo.PageResult;
import com.personaflow.commerce.order.service.OrderService;
import com.personaflow.commerce.order.vo.OrderAddressVO;
import com.personaflow.commerce.order.vo.OrderDetailVO;
import com.personaflow.commerce.order.vo.OrderItemVO;
import com.personaflow.commerce.order.vo.OrderListItemVO;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
        OrderControllerQueryTest.TestConfig.class
})
@EnableConfigurationProperties(JwtProperties.class)
@TestPropertySource(properties = {
        "commerce.jwt.secret=test-jwt-secret-with-at-least-thirty-two-characters",
        "commerce.jwt.expires-in=7200"
})
class OrderControllerQueryTest {

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
    void authenticatedUserCanListOrders() throws Exception {
        when(orderService.listOrders(20, 2, 5)).thenReturn(new PageResult<>(
                List.of(orderListItemVO()),
                2,
                5,
                12
        ));

        mockMvc.perform(get("/api/trade/orders")
                        .param("status", "20")
                        .param("page", "2")
                        .param("size", "5")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.size").value(5))
                .andExpect(jsonPath("$.data.total").value(12))
                .andExpect(jsonPath("$.data.records[0].orderId").value(50001))
                .andExpect(jsonPath("$.data.records[0].orderNo").value("PF20260628230000000123"))
                .andExpect(jsonPath("$.data.records[0].status").value(20));

        verify(orderService).listOrders(20, 2, 5);
    }

    @Test
    void authenticatedUserCanGetOrderDetail() throws Exception {
        when(orderService.getOrderDetail(50001L)).thenReturn(orderDetailVO());

        mockMvc.perform(get("/api/trade/orders/50001")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.orderId").value(50001))
                .andExpect(jsonPath("$.data.orderNo").value("PF20260628230000000123"))
                .andExpect(jsonPath("$.data.address.recipientName").value("Ada"))
                .andExpect(jsonPath("$.data.items[0].skuId").value(30001))
                .andExpect(jsonPath("$.data.items[0].productName").value("KeyForge K3"))
                .andExpect(jsonPath("$.data.payment").doesNotExist());

        verify(orderService).getOrderDetail(50001L);
    }

    @Test
    void anonymousUserCannotAccessOrderQueryEndpoints() throws Exception {
        mockMvc.perform(get("/api/trade/orders"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_UNAUTHORIZED"));

        mockMvc.perform(get("/api/trade/orders/50001"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_UNAUTHORIZED"));

        verifyNoInteractions(orderService);
    }

    @Test
    void missingOrderReturnsNotFound() throws Exception {
        when(orderService.getOrderDetail(99999L))
                .thenThrow(new BusinessException(ErrorCode.TRADE_ORDER_NOT_FOUND));

        mockMvc.perform(get("/api/trade/orders/99999")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.errorCode").value("TRADE_ORDER_NOT_FOUND"));

        verify(orderService).getOrderDetail(99999L);
    }

    private String bearerToken() {
        return "Bearer " + jwtService.generateAccessToken(10001L, Set.of("ROLE_USER"));
    }

    private OrderListItemVO orderListItemVO() {
        return new OrderListItemVO(
                50001L,
                "PF20260628230000000123",
                new BigDecimal("918.00"),
                20,
                LocalDateTime.of(2026, 6, 28, 23, 0),
                LocalDateTime.of(2026, 6, 28, 23, 30),
                null
        );
    }

    private OrderDetailVO orderDetailVO() {
        return new OrderDetailVO(
                50001L,
                "PF20260628230000000123",
                10001L,
                new OrderAddressVO(
                        1L,
                        "Ada",
                        "13800000000",
                        "Zhejiang",
                        "Hangzhou",
                        "Xihu",
                        "No. 1 West Lake Road",
                        "310000"
                ),
                new BigDecimal("918.00"),
                10,
                LocalDateTime.of(2026, 6, 28, 23, 0),
                null,
                null,
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
                )),
                null
        );
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
