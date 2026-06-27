package com.personaflow.commerce.auth.controller;

import com.personaflow.commerce.auth.service.AuthService;
import com.personaflow.commerce.auth.vo.RegisterVO;
import com.personaflow.commerce.common.error.BusinessException;
import com.personaflow.commerce.common.error.ErrorCode;
import com.personaflow.commerce.common.error.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerRegisterTest {

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void registerReturnsSuccessResponse() throws Exception {
        when(authService.register(any()))
                .thenReturn(new RegisterVO(10003L, "linjer_01", "Linjer"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "linjer_01",
                                  "password": "Example123!",
                                  "displayName": "Linjer"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.errorCode").doesNotExist())
                .andExpect(jsonPath("$.data.userId").value(10003))
                .andExpect(jsonPath("$.data.username").value("linjer_01"))
                .andExpect(jsonPath("$.data.displayName").value("Linjer"));
    }

    @Test
    void duplicateUsernameReturnsBusinessError() throws Exception {
        when(authService.register(any()))
                .thenThrow(new BusinessException(ErrorCode.ACCOUNT_USERNAME_EXISTS));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "linjer_01",
                                  "password": "Example123!",
                                  "displayName": "Linjer"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_USERNAME_EXISTS"));
    }

    @Test
    void invalidPasswordReturnsValidationError() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "linjer_01",
                                  "password": "abcdefgh",
                                  "displayName": "Linjer"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.errorCode").value("COMMON_VALIDATION_FAILED"));

        verifyNoInteractions(authService);
    }
}
