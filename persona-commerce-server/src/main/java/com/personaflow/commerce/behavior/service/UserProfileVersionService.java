package com.personaflow.commerce.behavior.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.personaflow.commerce.behavior.dto.UserProfileVersionCreateCommand;
import com.personaflow.commerce.behavior.entity.UserProfileVersionEntity;
import com.personaflow.commerce.behavior.mapper.UserProfileVersionMapper;
import org.springframework.dao.DuplicateKeyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserProfileVersionService {

    private static final Logger log = LoggerFactory.getLogger(UserProfileVersionService.class);
    private static final int MAX_AUTO_VERSION_RETRIES = 3;

    private final UserProfileVersionMapper userProfileVersionMapper;

    public UserProfileVersionService(UserProfileVersionMapper userProfileVersionMapper) {
        this.userProfileVersionMapper = userProfileVersionMapper;
    }

    @Transactional
    public UserProfileVersionEntity saveProfileVersion(UserProfileVersionCreateCommand command) {
        if (command.versionNo() != null) {
            return insertProfileVersion(command, command.versionNo());
        }

        DuplicateKeyException lastException = null;
        for (int attempt = 0; attempt < MAX_AUTO_VERSION_RETRIES; attempt++) {
            try {
                Integer versionNo = nextVersionNo(command.userId());
                UserProfileVersionEntity saved = insertProfileVersion(command, versionNo);
                log.info(
                        "Profile version saved userId={}, profileVersionId={}, versionNo={}, workflowId={}",
                        command.userId(),
                        saved.getId(),
                        versionNo,
                        command.sourceWorkflowId()
                );
                return saved;
            } catch (DuplicateKeyException exception) {
                lastException = exception;
                log.warn(
                        "Profile version conflict userId={}, attempt={}/{}",
                        command.userId(),
                        attempt + 1,
                        MAX_AUTO_VERSION_RETRIES
                );
            }
        }
        throw lastException;
    }

    private UserProfileVersionEntity insertProfileVersion(UserProfileVersionCreateCommand command, Integer versionNo) {
        UserProfileVersionEntity profileVersion = new UserProfileVersionEntity();
        profileVersion.setUserId(command.userId());
        profileVersion.setVersionNo(versionNo);
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

    private Integer nextVersionNo(Long userId) {
        UserProfileVersionEntity latest = userProfileVersionMapper.selectOne(
                Wrappers.<UserProfileVersionEntity>lambdaQuery()
                        .eq(UserProfileVersionEntity::getUserId, userId)
                        .orderByDesc(UserProfileVersionEntity::getVersionNo)
                        .last("LIMIT 1")
        );
        if (latest == null || latest.getVersionNo() == null) {
            return 1;
        }
        return latest.getVersionNo() + 1;
    }
}
