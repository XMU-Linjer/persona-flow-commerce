package com.personaflow.commerce.favorite.controller;

import com.personaflow.commerce.auth.security.JwtAuthenticationFilter;
import com.personaflow.commerce.auth.security.JwtProperties;
import com.personaflow.commerce.auth.security.JwtService;
import com.personaflow.commerce.auth.security.SecurityConfig;
import com.personaflow.commerce.common.error.GlobalExceptionHandler;
import com.personaflow.commerce.common.error.RestAccessDeniedHandler;
import com.personaflow.commerce.common.error.RestAuthenticationEntryPoint;
import com.personaflow.commerce.common.error.SecurityErrorResponseWriter;
import com.personaflow.commerce.common.vo.PageResult;
import com.personaflow.commerce.favorite.service.FavoriteService;
import com.personaflow.commerce.favorite.vo.FavoriteItemVO;
import com.personaflow.commerce.favorite.vo.FavoriteStatusVO;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FavoriteController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class,
        SecurityErrorResponseWriter.class,
        GlobalExceptionHandler.class,
        FavoriteControllerTest.TestConfig.class
})
@EnableConfigurationProperties(JwtProperties.class)
@TestPropertySource(properties = {
        "commerce.jwt.secret=test-jwt-secret-with-at-least-thirty-two-characters",
        "commerce.jwt.expires-in=7200"
})
class FavoriteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private FavoriteService favoriteService;

    @BeforeEach
    void setUp() {
        reset(favoriteService);
    }

    @Test
    void authenticatedUserCanAddFavorite() throws Exception {
        when(favoriteService.addFavorite(30001L))
                .thenReturn(new FavoriteStatusVO(30001L, true));

        mockMvc.perform(post("/api/shopping/favorites/30001")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.skuId").value(30001))
                .andExpect(jsonPath("$.data.favorited").value(true));

        verify(favoriteService).addFavorite(30001L);
    }

    @Test
    void authenticatedUserCanRemoveFavorite() throws Exception {
        when(favoriteService.removeFavorite(30001L))
                .thenReturn(new FavoriteStatusVO(30001L, false));

        mockMvc.perform(delete("/api/shopping/favorites/30001")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.skuId").value(30001))
                .andExpect(jsonPath("$.data.favorited").value(false));

        verify(favoriteService).removeFavorite(30001L);
    }

    @Test
    void authenticatedUserCanListFavorites() throws Exception {
        when(favoriteService.listFavorites(2, 5)).thenReturn(new PageResult<>(
                List.of(favoriteItemVO()),
                2,
                5,
                12
        ));

        mockMvc.perform(get("/api/shopping/favorites")
                        .param("page", "2")
                        .param("size", "5")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.size").value(5))
                .andExpect(jsonPath("$.data.total").value(12))
                .andExpect(jsonPath("$.data.records[0].favoriteId").value(10))
                .andExpect(jsonPath("$.data.records[0].skuId").value(30001))
                .andExpect(jsonPath("$.data.records[0].productName").value("KeyForge K3"));

        verify(favoriteService).listFavorites(2, 5);
    }

    @Test
    void anonymousUserCannotAccessFavoriteEndpoints() throws Exception {
        mockMvc.perform(post("/api/shopping/favorites/30001"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_UNAUTHORIZED"));

        mockMvc.perform(delete("/api/shopping/favorites/30001"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_UNAUTHORIZED"));

        mockMvc.perform(get("/api/shopping/favorites"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_UNAUTHORIZED"));

        verifyNoInteractions(favoriteService);
    }

    private String bearerToken() {
        return "Bearer " + jwtService.generateAccessToken(10001L, Set.of("ROLE_USER"));
    }

    private FavoriteItemVO favoriteItemVO() {
        return new FavoriteItemVO(
                10L,
                30001L,
                20001L,
                201L,
                "键盘鼠标",
                "KeyForge K3",
                "青轴 白色",
                new BigDecimal("459.00"),
                "sku-30001.jpg",
                LocalDateTime.of(2026, 6, 28, 10, 30)
        );
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        FavoriteService favoriteService() {
            return mock(FavoriteService.class);
        }

        @Bean
        JwtService jwtService(JwtProperties properties) {
            return new JwtService(properties);
        }
    }
}
