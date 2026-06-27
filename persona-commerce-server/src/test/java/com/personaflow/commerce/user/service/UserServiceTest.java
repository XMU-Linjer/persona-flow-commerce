package com.personaflow.commerce.user.service;

import com.personaflow.commerce.common.error.BusinessException;
import com.personaflow.commerce.common.error.ErrorCode;
import com.personaflow.commerce.user.api.CurrentUserProvider;
import com.personaflow.commerce.user.api.model.CurrentUser;
import com.personaflow.commerce.user.dto.ChangePasswordRequest;
import com.personaflow.commerce.user.dto.UpdateProfileRequest;
import com.personaflow.commerce.user.entity.LoginIdentityEntity;
import com.personaflow.commerce.user.entity.UserEntity;
import com.personaflow.commerce.user.mapper.LoginIdentityMapper;
import com.personaflow.commerce.user.mapper.UserMapper;
import com.personaflow.commerce.user.vo.CurrentUserVO;
import com.personaflow.commerce.user.vo.UserProfileVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private UserMapper userMapper;

    @Mock
    private LoginIdentityMapper loginIdentityMapper;

    @InjectMocks
    private UserService userService;

    @Test
    void getCurrentUserReturnsUserProfileAndRoles() {
        Set<String> roles = new LinkedHashSet<>(List.of("ROLE_USER", "ROLE_ADMIN"));
        when(currentUserProvider.requireCurrentUser()).thenReturn(new CurrentUser(10001L, roles));
        when(userMapper.selectById(10001L)).thenReturn(user(10001L, "Linjer", null, "Example123!"));
        when(loginIdentityMapper.selectOne(any())).thenReturn(loginIdentity(10001L, "linjer_01"));

        CurrentUserVO result = userService.getCurrentUser();

        assertThat(result.userId()).isEqualTo(10001L);
        assertThat(result.username()).isEqualTo("linjer_01");
        assertThat(result.displayName()).isEqualTo("Linjer");
        assertThat(result.avatarUrl()).isNull();
        assertThat(result.roles()).containsExactly("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    void getCurrentUserThrowsWhenUserDoesNotExist() {
        when(currentUserProvider.requireCurrentUser()).thenReturn(new CurrentUser(10001L, Set.of("ROLE_USER")));
        when(userMapper.selectById(10001L)).thenReturn(null);

        assertBusinessError(
                () -> userService.getCurrentUser(),
                ErrorCode.ACCOUNT_USER_NOT_FOUND
        );
    }

    @Test
    void updateProfileUpdatesOnlyDisplayNameAndAvatarUrl() {
        when(currentUserProvider.requireCurrentUser()).thenReturn(new CurrentUser(10001L, Set.of("ROLE_USER")));
        when(userMapper.selectById(10001L)).thenReturn(user(10001L, "Old Name", "old.png", "Example123!"));
        when(userMapper.update(isNull(), any())).thenReturn(1);

        UserProfileVO result = userService.updateProfile(
                new UpdateProfileRequest(" New Name ", " ")
        );

        assertThat(result.userId()).isEqualTo(10001L);
        assertThat(result.displayName()).isEqualTo("New Name");
        assertThat(result.avatarUrl()).isNull();
        verify(userMapper).update(isNull(), any());
        verify(loginIdentityMapper, never()).selectOne(any());
    }

    @Test
    void changePasswordUpdatesBcryptPasswordWhenOldPasswordMatches() {
        when(currentUserProvider.requireCurrentUser()).thenReturn(new CurrentUser(10001L, Set.of("ROLE_USER")));
        when(userMapper.selectById(10001L)).thenReturn(user(10001L, "Linjer", null, "OldPass123"));
        when(userMapper.updateById(any(UserEntity.class))).thenReturn(1);

        userService.changePassword(new ChangePasswordRequest("OldPass123", "NewPass456"));

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userMapper).updateById(userCaptor.capture());
        UserEntity updatedUser = userCaptor.getValue();
        assertThat(updatedUser.getId()).isEqualTo(10001L);
        assertThat(updatedUser.getPasswordHash()).isNotEqualTo("NewPass456");
        assertThat(new BCryptPasswordEncoder().matches("NewPass456", updatedUser.getPasswordHash())).isTrue();
    }

    @Test
    void changePasswordThrowsWhenOldPasswordDoesNotMatch() {
        when(currentUserProvider.requireCurrentUser()).thenReturn(new CurrentUser(10001L, Set.of("ROLE_USER")));
        when(userMapper.selectById(10001L)).thenReturn(user(10001L, "Linjer", null, "OldPass123"));

        assertBusinessError(
                () -> userService.changePassword(new ChangePasswordRequest("WrongPass123", "NewPass456")),
                ErrorCode.ACCOUNT_CURRENT_PASSWORD_INVALID
        );
        verify(userMapper, never()).updateById(any(UserEntity.class));
    }

    private UserEntity user(Long id, String displayName, String avatarUrl, String rawPassword) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setDisplayName(displayName);
        user.setAvatarUrl(avatarUrl);
        user.setPasswordHash(new BCryptPasswordEncoder().encode(rawPassword));
        return user;
    }

    private LoginIdentityEntity loginIdentity(Long userId, String username) {
        LoginIdentityEntity identity = new LoginIdentityEntity();
        identity.setUserId(userId);
        identity.setIdentityType("USERNAME");
        identity.setIdentifier(username);
        identity.setNormalizedIdentifier(username);
        identity.setVerified(true);
        return identity;
    }

    private void assertBusinessError(Runnable operation, ErrorCode expectedErrorCode) {
        assertThatThrownBy(operation::run)
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(expectedErrorCode);
    }
}
