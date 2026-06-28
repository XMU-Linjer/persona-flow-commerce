package com.personaflow.commerce.cart.controller;

import com.personaflow.commerce.auth.security.JwtAuthenticationFilter;
import com.personaflow.commerce.auth.security.JwtProperties;
import com.personaflow.commerce.auth.security.JwtService;
import com.personaflow.commerce.auth.security.SecurityConfig;
import com.personaflow.commerce.cart.dto.AddCartItemRequest;
import com.personaflow.commerce.cart.dto.UpdateCartItemRequest;
import com.personaflow.commerce.cart.service.CartService;
import com.personaflow.commerce.cart.vo.CartItemSimpleVO;
import com.personaflow.commerce.cart.vo.CartItemVO;
import com.personaflow.commerce.common.error.GlobalExceptionHandler;
import com.personaflow.commerce.common.error.RestAccessDeniedHandler;
import com.personaflow.commerce.common.error.RestAuthenticationEntryPoint;
import com.personaflow.commerce.common.error.SecurityErrorResponseWriter;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CartController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class,
        SecurityErrorResponseWriter.class,
        GlobalExceptionHandler.class,
        CartControllerTest.TestConfig.class
})
@EnableConfigurationProperties(JwtProperties.class)
@TestPropertySource(properties = {
        "commerce.jwt.secret=test-jwt-secret-with-at-least-thirty-two-characters",
        "commerce.jwt.expires-in=7200"
})
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CartService cartService;

    @BeforeEach
    void setUp() {
        reset(cartService);
    }

    @Test
    void authenticatedUserCanAddCartItem() throws Exception {
        when(cartService.addCartItem(new AddCartItemRequest(30001L, 2)))
                .thenReturn(new CartItemSimpleVO(10L, 30001L, 2));

        mockMvc.perform(post("/api/shopping/cart/items")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "skuId": 30001,
                                  "quantity": 2
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.cartItemId").value(10))
                .andExpect(jsonPath("$.data.skuId").value(30001))
                .andExpect(jsonPath("$.data.quantity").value(2));

        verify(cartService).addCartItem(new AddCartItemRequest(30001L, 2));
    }

    @Test
    void authenticatedUserCanUpdateCartItem() throws Exception {
        when(cartService.updateCartItem(eq(10L), eq(new UpdateCartItemRequest(3))))
                .thenReturn(new CartItemSimpleVO(10L, 30001L, 3));

        mockMvc.perform(patch("/api/shopping/cart/items/10")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "quantity": 3
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.cartItemId").value(10))
                .andExpect(jsonPath("$.data.quantity").value(3));

        verify(cartService).updateCartItem(10L, new UpdateCartItemRequest(3));
    }

    @Test
    void authenticatedUserCanDeleteCartItem() throws Exception {
        mockMvc.perform(delete("/api/shopping/cart/items/10")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(cartService).deleteCartItem(10L);
    }

    @Test
    void authenticatedUserCanClearCart() throws Exception {
        mockMvc.perform(delete("/api/shopping/cart/items")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(cartService).clearCart();
    }

    @Test
    void authenticatedUserCanListCartItems() throws Exception {
        when(cartService.listCartItems()).thenReturn(List.of(cartItemVO()));

        mockMvc.perform(get("/api/shopping/cart/items")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].cartItemId").value(10))
                .andExpect(jsonPath("$.data[0].skuId").value(30001))
                .andExpect(jsonPath("$.data[0].productName").value("KeyForge K3"))
                .andExpect(jsonPath("$.data[0].quantity").value(2))
                .andExpect(jsonPath("$.data[0].subtotal").value(918.00));

        verify(cartService).listCartItems();
    }

    @Test
    void anonymousUserCannotAccessCartEndpoints() throws Exception {
        mockMvc.perform(post("/api/shopping/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_UNAUTHORIZED"));

        mockMvc.perform(patch("/api/shopping/cart/items/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_UNAUTHORIZED"));

        mockMvc.perform(delete("/api/shopping/cart/items/10"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_UNAUTHORIZED"));

        mockMvc.perform(delete("/api/shopping/cart/items"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_UNAUTHORIZED"));

        mockMvc.perform(get("/api/shopping/cart/items"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_UNAUTHORIZED"));

        verifyNoInteractions(cartService);
    }

    private String bearerToken() {
        return "Bearer " + jwtService.generateAccessToken(10001L, Set.of("ROLE_USER"));
    }

    private CartItemVO cartItemVO() {
        return new CartItemVO(
                10L,
                30001L,
                20001L,
                201L,
                "键盘鼠标",
                "KeyForge K3",
                "青轴 白色",
                new BigDecimal("459.00"),
                "sku-30001.jpg",
                2,
                new BigDecimal("918.00"),
                LocalDateTime.of(2026, 6, 28, 10, 30),
                LocalDateTime.of(2026, 6, 28, 11, 30)
        );
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        CartService cartService() {
            return mock(CartService.class);
        }

        @Bean
        JwtService jwtService(JwtProperties properties) {
            return new JwtService(properties);
        }
    }
}
