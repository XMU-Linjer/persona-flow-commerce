package com.personaflow.commerce.auth.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.personaflow.commerce.auth.dto.LoginRequest;
import com.personaflow.commerce.auth.dto.RegisterRequest;
import com.personaflow.commerce.auth.security.JwtService;
import com.personaflow.commerce.auth.support.LoginIdentityNormalizer;
import com.personaflow.commerce.auth.vo.LoginVO;
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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class AuthService {

    private static final String USERNAME_IDENTITY_TYPE = "USERNAME";
    private static final String ROLE_USER = "ROLE_USER";

    private final UserMapper userMapper;
    private final LoginIdentityMapper loginIdentityMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(
            UserMapper userMapper,
            LoginIdentityMapper loginIdentityMapper,
            RoleMapper roleMapper,
            UserRoleMapper userRoleMapper,
            JwtService jwtService
    ) {
        this.userMapper = userMapper;
        this.loginIdentityMapper = loginIdentityMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.jwtService = jwtService;
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

    public LoginVO login(LoginRequest request) {
        String identityType = normalizeIdentityType(request.identityType());
        if (!USERNAME_IDENTITY_TYPE.equals(identityType)) {
            throw new BusinessException(ErrorCode.ACCOUNT_UNSUPPORTED_IDENTITY_TYPE);
        }

        String normalizedIdentifier = LoginIdentityNormalizer.normalizeUsername(request.identifier());
        LoginIdentityEntity identity = loginIdentityMapper.selectOne(
                Wrappers.<LoginIdentityEntity>lambdaQuery()
                        .eq(LoginIdentityEntity::getIdentityType, identityType)
                        .eq(LoginIdentityEntity::getNormalizedIdentifier, normalizedIdentifier)
        );
        if (identity == null) {
            throw invalidCredentials();
        }

        UserEntity user = userMapper.selectById(identity.getUserId());
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }

        Set<String> roles = loadRoleCodes(user.getId());
        String accessToken = jwtService.generateAccessToken(user.getId(), roles);
        return new LoginVO(
                accessToken,
                "Bearer",
                jwtService.expiresIn(),
                new LoginVO.LoginUserVO(
                        user.getId(),
                        identity.getIdentifier(),
                        user.getDisplayName(),
                        roles
                )
        );
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

    private Set<String> loadRoleCodes(Long userId) {
        List<UserRoleEntity> userRoles = userRoleMapper.selectList(
                Wrappers.<UserRoleEntity>lambdaQuery()
                        .eq(UserRoleEntity::getUserId, userId)
        );
        List<Long> roleIds = userRoles.stream()
                .map(UserRoleEntity::getRoleId)
                .toList();
        if (roleIds.isEmpty()) {
            return Set.of();
        }

        List<RoleEntity> roles = roleMapper.selectList(
                Wrappers.<RoleEntity>lambdaQuery()
                        .in(RoleEntity::getId, roleIds)
        );
        Set<String> roleCodes = new LinkedHashSet<>();
        for (RoleEntity role : roles) {
            roleCodes.add(role.getCode());
        }
        return roleCodes;
    }

    private String normalizeIdentityType(String identityType) {
        return identityType.trim().toUpperCase(Locale.ROOT);
    }

    private BusinessException invalidCredentials() {
        return new BusinessException(ErrorCode.ACCOUNT_INVALID_CREDENTIALS);
    }

    private void expectOneRow(int affectedRows, String message) {
        if (affectedRows != 1) {
            throw new IllegalStateException(message);
        }
    }
}
