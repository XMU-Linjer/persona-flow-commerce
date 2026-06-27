package com.personaflow.commerce.user.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private static final String USERNAME_IDENTITY_TYPE = "USERNAME";

    private final CurrentUserProvider currentUserProvider;
    private final UserMapper userMapper;
    private final LoginIdentityMapper loginIdentityMapper;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(
            CurrentUserProvider currentUserProvider,
            UserMapper userMapper,
            LoginIdentityMapper loginIdentityMapper
    ) {
        this.currentUserProvider = currentUserProvider;
        this.userMapper = userMapper;
        this.loginIdentityMapper = loginIdentityMapper;
    }

    public CurrentUserVO getCurrentUser() {
        CurrentUser currentUser = currentUserProvider.requireCurrentUser();
        UserEntity user = requireUser(currentUser.userId());
        LoginIdentityEntity identity = requireUsernameIdentity(currentUser.userId());

        return new CurrentUserVO(
                currentUser.userId(),
                identity.getIdentifier(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                currentUser.roles()
        );
    }

    @Transactional
    public UserProfileVO updateProfile(UpdateProfileRequest request) {
        CurrentUser currentUser = currentUserProvider.requireCurrentUser();
        requireUser(currentUser.userId());

        String displayName = request.displayName().trim();
        String avatarUrl = normalizeAvatarUrl(request.avatarUrl());
        int affectedRows = userMapper.update(
                null,
                Wrappers.<UserEntity>update()
                        .eq("id", currentUser.userId())
                        .set("display_name", displayName)
                        .set("avatar_url", avatarUrl)
        );
        expectOneRow(affectedRows, "Failed to update user profile");

        return new UserProfileVO(currentUser.userId(), displayName, avatarUrl);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        CurrentUser currentUser = currentUserProvider.requireCurrentUser();
        UserEntity user = requireUser(currentUser.userId());
        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.ACCOUNT_CURRENT_PASSWORD_INVALID);
        }

        UserEntity updatedUser = new UserEntity();
        updatedUser.setId(currentUser.userId());
        updatedUser.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        expectOneRow(userMapper.updateById(updatedUser), "Failed to update user password");
    }

    private UserEntity requireUser(Long userId) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.ACCOUNT_USER_NOT_FOUND);
        }
        return user;
    }

    private LoginIdentityEntity requireUsernameIdentity(Long userId) {
        LoginIdentityEntity identity = loginIdentityMapper.selectOne(
                Wrappers.<LoginIdentityEntity>lambdaQuery()
                        .eq(LoginIdentityEntity::getUserId, userId)
                        .eq(LoginIdentityEntity::getIdentityType, USERNAME_IDENTITY_TYPE)
        );
        if (identity == null) {
            throw new BusinessException(ErrorCode.ACCOUNT_USER_NOT_FOUND);
        }
        return identity;
    }

    private String normalizeAvatarUrl(String avatarUrl) {
        if (avatarUrl == null) {
            return null;
        }
        String trimmed = avatarUrl.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void expectOneRow(int affectedRows, String message) {
        if (affectedRows != 1) {
            throw new IllegalStateException(message);
        }
    }
}
