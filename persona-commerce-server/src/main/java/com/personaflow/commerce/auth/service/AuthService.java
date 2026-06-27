package com.personaflow.commerce.auth.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.personaflow.commerce.auth.dto.RegisterRequest;
import com.personaflow.commerce.auth.support.LoginIdentityNormalizer;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final String USERNAME_IDENTITY_TYPE = "USERNAME";
    private static final String ROLE_USER = "ROLE_USER";

    private final UserMapper userMapper;
    private final LoginIdentityMapper loginIdentityMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(
            UserMapper userMapper,
            LoginIdentityMapper loginIdentityMapper,
            RoleMapper roleMapper,
            UserRoleMapper userRoleMapper
    ) {
        this.userMapper = userMapper;
        this.loginIdentityMapper = loginIdentityMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
    }

    @Transactional
    public RegisterVO register(RegisterRequest request) {
        String username = request.username().trim();
        String normalizedIdentifier = LoginIdentityNormalizer.normalizeUsername(username);
        ensureUsernameAvailable(normalizedIdentifier);

        UserEntity user = new UserEntity();
        user.setDisplayName(request.displayName().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        expectOneRow(userMapper.insert(user), "Failed to create user");

        LoginIdentityEntity identity = new LoginIdentityEntity();
        identity.setUserId(user.getId());
        identity.setIdentityType(USERNAME_IDENTITY_TYPE);
        identity.setIdentifier(username);
        identity.setNormalizedIdentifier(normalizedIdentifier);
        identity.setVerified(true);
        expectOneRow(loginIdentityMapper.insert(identity), "Failed to create login identity");

        RoleEntity role = findUserRole();
        UserRoleEntity userRole = new UserRoleEntity();
        userRole.setUserId(user.getId());
        userRole.setRoleId(role.getId());
        expectOneRow(userRoleMapper.insert(userRole), "Failed to assign user role");

        return new RegisterVO(user.getId(), username, user.getDisplayName());
    }

    private void ensureUsernameAvailable(String normalizedIdentifier) {
        Long count = loginIdentityMapper.selectCount(
                Wrappers.<LoginIdentityEntity>lambdaQuery()
                        .eq(LoginIdentityEntity::getIdentityType, USERNAME_IDENTITY_TYPE)
                        .eq(LoginIdentityEntity::getNormalizedIdentifier, normalizedIdentifier)
        );
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.ACCOUNT_USERNAME_EXISTS);
        }
    }

    private RoleEntity findUserRole() {
        RoleEntity role = roleMapper.selectOne(
                Wrappers.<RoleEntity>lambdaQuery()
                        .eq(RoleEntity::getCode, ROLE_USER)
        );
        if (role == null) {
            throw new IllegalStateException("ROLE_USER is not initialized");
        }
        return role;
    }

    private void expectOneRow(int affectedRows, String message) {
        if (affectedRows != 1) {
            throw new IllegalStateException(message);
        }
    }
}
