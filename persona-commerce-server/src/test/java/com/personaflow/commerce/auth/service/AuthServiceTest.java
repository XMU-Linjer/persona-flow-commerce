package com.personaflow.commerce.auth.service;

import com.personaflow.commerce.auth.dto.RegisterRequest;
import com.personaflow.commerce.auth.vo.RegisterVO;
import com.personaflow.commerce.common.error.BusinessException;
import com.personaflow.commerce.common.error.ErrorCode;
import com.personaflow.commerce.user.entity.LoginIdentityEntity;
import com.personaflow.commerce.user.entity.RoleEntity;
import com.personaflow.commerce.user.entity.UserEntity;
import com.personaflow.commerce.user.entity.UserRoleEntity;
import com.personaflow.commerce.user.mapper.LoginIdentityMapper;
import com.personaflow.commerce.user.mapper.RoleMapper;
import com.personaflow.commerce.user.mapper.UserMapper;
import com.personaflow.commerce.user.mapper.UserRoleMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private LoginIdentityMapper loginIdentityMapper;

    @Mock
    private RoleMapper roleMapper;

    @Mock
    private UserRoleMapper userRoleMapper;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerCreatesUserLoginIdentityAndUserRole() {
        when(loginIdentityMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.insert(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setId(10003L);
            return 1;
        });
        when(loginIdentityMapper.insert(any(LoginIdentityEntity.class))).thenReturn(1);
        when(roleMapper.selectOne(any())).thenReturn(userRole());
        when(userRoleMapper.insert(any(UserRoleEntity.class))).thenReturn(1);

        RegisterVO result = authService.register(
                new RegisterRequest("LinJer_01", "Example123!", "Linjer")
        );

        assertThat(result.userId()).isEqualTo(10003L);
        assertThat(result.username()).isEqualTo("LinJer_01");
        assertThat(result.displayName()).isEqualTo("Linjer");

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userMapper).insert(userCaptor.capture());
        UserEntity user = userCaptor.getValue();
        assertThat(user.getDisplayName()).isEqualTo("Linjer");
        assertThat(user.getPasswordHash()).isNotEqualTo("Example123!");
        assertThat(new BCryptPasswordEncoder().matches("Example123!", user.getPasswordHash())).isTrue();

        ArgumentCaptor<LoginIdentityEntity> identityCaptor = ArgumentCaptor.forClass(LoginIdentityEntity.class);
        verify(loginIdentityMapper).insert(identityCaptor.capture());
        LoginIdentityEntity identity = identityCaptor.getValue();
        assertThat(identity.getUserId()).isEqualTo(10003L);
        assertThat(identity.getIdentityType()).isEqualTo("USERNAME");
        assertThat(identity.getIdentifier()).isEqualTo("LinJer_01");
        assertThat(identity.getNormalizedIdentifier()).isEqualTo("linjer_01");
        assertThat(identity.getVerified()).isTrue();

        ArgumentCaptor<UserRoleEntity> userRoleCaptor = ArgumentCaptor.forClass(UserRoleEntity.class);
        verify(userRoleMapper).insert(userRoleCaptor.capture());
        UserRoleEntity userRole = userRoleCaptor.getValue();
        assertThat(userRole.getUserId()).isEqualTo(10003L);
        assertThat(userRole.getRoleId()).isEqualTo(1L);
    }

    @Test
    void duplicateUsernameThrowsBusinessException() {
        when(loginIdentityMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("linjer_01", "Example123!", "Linjer")
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(ErrorCode.ACCOUNT_USERNAME_EXISTS);

        verify(userMapper, never()).insert(any(UserEntity.class));
        verify(loginIdentityMapper, never()).insert(any(LoginIdentityEntity.class));
        verifyNoInteractions(roleMapper, userRoleMapper);
    }

    @Test
    void usernameWithDifferentCaseUsesSameNormalizedIdentifier() {
        when(loginIdentityMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.insert(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setId(10004L);
            return 1;
        });
        when(loginIdentityMapper.insert(any(LoginIdentityEntity.class))).thenReturn(1);
        when(roleMapper.selectOne(any())).thenReturn(userRole());
        when(userRoleMapper.insert(any(UserRoleEntity.class))).thenReturn(1);

        authService.register(new RegisterRequest("DEMO_User", "Example123!", "Demo"));

        ArgumentCaptor<LoginIdentityEntity> identityCaptor = ArgumentCaptor.forClass(LoginIdentityEntity.class);
        verify(loginIdentityMapper).insert(identityCaptor.capture());
        assertThat(identityCaptor.getValue().getIdentifier()).isEqualTo("DEMO_User");
        assertThat(identityCaptor.getValue().getNormalizedIdentifier()).isEqualTo("demo_user");
    }

    private RoleEntity userRole() {
        RoleEntity role = new RoleEntity();
        role.setId(1L);
        role.setCode("ROLE_USER");
        role.setName("User");
        return role;
    }
}
