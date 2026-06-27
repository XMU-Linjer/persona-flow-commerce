package com.personaflow.commerce.user.controller;

import com.personaflow.commerce.common.error.BusinessException;
import com.personaflow.commerce.common.error.ErrorCode;
import com.personaflow.commerce.common.error.GlobalExceptionHandler;
import com.personaflow.commerce.user.dto.ChangePasswordRequest;
import com.personaflow.commerce.user.dto.UpdateProfileRequest;
import com.personaflow.commerce.user.service.UserService;
import com.personaflow.commerce.user.vo.CurrentUserVO;
import com.personaflow.commerce.user.vo.UserProfileVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new UserController(userService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getCurrentUserReturnsSuccessResponse() throws Exception {
        when(userService.getCurrentUser())
                .thenReturn(new CurrentUserVO(10001L, "linjer_01", "Linjer", null, Set.of("ROLE_USER")));

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userId").value(10001))
                .andExpect(jsonPath("$.data.username").value("linjer_01"))
                .andExpect(jsonPath("$.data.displayName").value("Linjer"))
                .andExpect(jsonPath("$.data.avatarUrl").doesNotExist())
                .andExpect(jsonPath("$.data.roles[0]").value("ROLE_USER"));
    }

    @Test
    void updateProfileIgnoresUsernameRolesAndPasswordFields() throws Exception {
        when(userService.updateProfile(any()))
                .thenReturn(new UserProfileVO(10001L, "New Name", "https://example.com/avatar.png"));

        mockMvc.perform(patch("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "New Name",
                                  "avatarUrl": "https://example.com/avatar.png",
                                  "username": "hacker",
                                  "roles": ["ROLE_ADMIN"],
                                  "password": "Password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userId").value(10001))
                .andExpect(jsonPath("$.data.displayName").value("New Name"))
                .andExpect(jsonPath("$.data.avatarUrl").value("https://example.com/avatar.png"))
                .andExpect(jsonPath("$.data.username").doesNotExist())
                .andExpect(jsonPath("$.data.roles").doesNotExist())
                .andExpect(jsonPath("$.data.password").doesNotExist());

        ArgumentCaptor<UpdateProfileRequest> requestCaptor = ArgumentCaptor.forClass(UpdateProfileRequest.class);
        verify(userService).updateProfile(requestCaptor.capture());
        assertThat(requestCaptor.getValue().displayName()).isEqualTo("New Name");
        assertThat(requestCaptor.getValue().avatarUrl()).isEqualTo("https://example.com/avatar.png");
    }

    @Test
    void invalidProfileReturnsValidationError() throws Exception {
        mockMvc.perform(patch("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "",
                                  "avatarUrl": "https://example.com/avatar.png"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.errorCode").value("COMMON_VALIDATION_FAILED"));

        verifyNoInteractions(userService);
    }

    @Test
    void changePasswordReturnsSuccessResponse() throws Exception {
        mockMvc.perform(put("/api/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "oldPassword": "OldPass123",
                                  "newPassword": "NewPass456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").doesNotExist());

        ArgumentCaptor<ChangePasswordRequest> requestCaptor = ArgumentCaptor.forClass(ChangePasswordRequest.class);
        verify(userService).changePassword(requestCaptor.capture());
        assertThat(requestCaptor.getValue().oldPassword()).isEqualTo("OldPass123");
        assertThat(requestCaptor.getValue().newPassword()).isEqualTo("NewPass456");
    }

    @Test
    void changePasswordWithWrongOldPasswordReturnsBusinessError() throws Exception {
        doThrow(new BusinessException(ErrorCode.ACCOUNT_CURRENT_PASSWORD_INVALID))
                .when(userService)
                .changePassword(any());

        mockMvc.perform(put("/api/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "oldPassword": "WrongPass123",
                                  "newPassword": "NewPass456"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_CURRENT_PASSWORD_INVALID"));
    }

    @Test
    void invalidNewPasswordReturnsValidationError() throws Exception {
        mockMvc.perform(put("/api/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "oldPassword": "OldPass123",
                                  "newPassword": "abcdefgh"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.errorCode").value("COMMON_VALIDATION_FAILED"));

        verifyNoInteractions(userService);
    }
}
