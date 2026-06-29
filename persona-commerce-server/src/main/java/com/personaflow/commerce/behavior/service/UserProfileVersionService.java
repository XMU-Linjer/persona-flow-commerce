package com.personaflow.commerce.behavior.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.personaflow.commerce.behavior.dto.UserProfileVersionCreateCommand;
import com.personaflow.commerce.behavior.entity.UserProfileVersionEntity;
import com.personaflow.commerce.behavior.mapper.UserProfileVersionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserProfileVersionService {

    private final UserProfileVersionMapper userProfileVersionMapper;

    public UserProfileVersionService(UserProfileVersionMapper userProfileVersionMapper) {
        this.userProfileVersionMapper = userProfileVersionMapper;
    }

    @Transactional
    public UserProfileVersionEntity saveProfileVersion(UserProfileVersionCreateCommand command) {
        UserProfileVersionEntity profileVersion = new UserProfileVersionEntity();
        profileVersion.setUserId(command.userId());
        profileVersion.setVersionNo(command.versionNo());
        profileVersion.setProfileJson(command.profileJson());
        profileVersion.setSummary(command.summary());
        profileVersion.setSourceWorkflowId(command.sourceWorkflowId());
        userProfileVersionMapper.insert(profileVersion);
        return profileVersion;
    }

    @Transactional(readOnly = true)
    public Optional<UserProfileVersionEntity> findLatestProfile(Long userId) {
        return Optional.ofNullable(userProfileVersionMapper.selectOne(
                Wrappers.<UserProfileVersionEntity>lambdaQuery()
                        .eq(UserProfileVersionEntity::getUserId, userId)
                        .orderByDesc(UserProfileVersionEntity::getCreatedAt)
                        .orderByDesc(UserProfileVersionEntity::getId)
                        .last("LIMIT 1")
        ));
    }
}
