package com.personaflow.commerce.behavior.service;

import com.personaflow.commerce.behavior.dto.UserProfileVersionCreateCommand;
import com.personaflow.commerce.behavior.entity.UserProfileVersionEntity;
import com.personaflow.commerce.behavior.mapper.UserProfileVersionMapper;
import org.springframework.dao.DuplicateKeyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileVersionServiceTest {

    @Mock
    private UserProfileVersionMapper userProfileVersionMapper;

    private UserProfileVersionService userProfileVersionService;

    @BeforeEach
    void setUp() {
        userProfileVersionService = new UserProfileVersionService(userProfileVersionMapper);
    }

    @Test
    void saveProfileVersionInsertsVersion() {
        when(userProfileVersionMapper.insert(any(UserProfileVersionEntity.class))).thenAnswer(invocation -> {
            UserProfileVersionEntity profileVersion = invocation.getArgument(0);
            profileVersion.setId(1L);
            return 1;
        });

        UserProfileVersionEntity result = userProfileVersionService.saveProfileVersion(command(1));

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUserId()).isEqualTo(10001L);
        assertThat(result.getVersionNo()).isEqualTo(1);

        ArgumentCaptor<UserProfileVersionEntity> profileCaptor = ArgumentCaptor.forClass(UserProfileVersionEntity.class);
        verify(userProfileVersionMapper).insert(profileCaptor.capture());
        UserProfileVersionEntity inserted = profileCaptor.getValue();
        assertThat(inserted.getProfileJson()).contains("preferenceScore");
        assertThat(inserted.getSummary()).contains("keyboard accessories");
        assertThat(inserted.getSourceWorkflowId()).isEqualTo("workflow-001");
    }

    @Test
    void saveProfileVersionCanPersistMultipleVersions() {
        when(userProfileVersionMapper.insert(any(UserProfileVersionEntity.class))).thenReturn(1);

        userProfileVersionService.saveProfileVersion(command(1));
        userProfileVersionService.saveProfileVersion(command(2));

        ArgumentCaptor<UserProfileVersionEntity> profileCaptor = ArgumentCaptor.forClass(UserProfileVersionEntity.class);
        verify(userProfileVersionMapper, org.mockito.Mockito.times(2)).insert(profileCaptor.capture());
        assertThat(profileCaptor.getAllValues())
                .extracting(UserProfileVersionEntity::getVersionNo)
                .containsExactly(1, 2);
    }

    @Test
    void saveProfileVersionAllocatesNextVersionWhenCommandVersionIsNull() {
        when(userProfileVersionMapper.selectOne(any())).thenReturn(profileVersion(2));
        when(userProfileVersionMapper.insert(any(UserProfileVersionEntity.class))).thenAnswer(invocation -> {
            UserProfileVersionEntity profileVersion = invocation.getArgument(0);
            profileVersion.setId(3L);
            return 1;
        });

        UserProfileVersionEntity result = userProfileVersionService.saveProfileVersion(command(null));

        assertThat(result.getVersionNo()).isEqualTo(3);
        verify(userProfileVersionMapper).selectOne(any());
    }

    @Test
    void saveProfileVersionRetriesWhenAutoVersionConflicts() {
        when(userProfileVersionMapper.selectOne(any()))
                .thenReturn(profileVersion(2))
                .thenReturn(profileVersion(3));
        when(userProfileVersionMapper.insert(any(UserProfileVersionEntity.class)))
                .thenThrow(new DuplicateKeyException("duplicate version"))
                .thenAnswer(invocation -> {
                    UserProfileVersionEntity profileVersion = invocation.getArgument(0);
                    profileVersion.setId(4L);
                    return 1;
                });

        UserProfileVersionEntity result = userProfileVersionService.saveProfileVersion(command(null));

        assertThat(result.getVersionNo()).isEqualTo(4);
        verify(userProfileVersionMapper, times(2)).selectOne(any());
        verify(userProfileVersionMapper, times(2)).insert(any(UserProfileVersionEntity.class));
    }

    @Test
    void findLatestProfileReturnsNewestProfile() {
        UserProfileVersionEntity latestProfile = profileVersion(2);
        when(userProfileVersionMapper.selectOne(any())).thenReturn(latestProfile);

        Optional<UserProfileVersionEntity> result = userProfileVersionService.findLatestProfile(10001L);

        assertThat(result).containsSame(latestProfile);
        assertThat(result.get().getVersionNo()).isEqualTo(2);
        verify(userProfileVersionMapper).selectOne(any());
    }

    @Test
    void findLatestProfileReturnsEmptyWhenNoProfileExists() {
        when(userProfileVersionMapper.selectOne(any())).thenReturn(null);

        Optional<UserProfileVersionEntity> result = userProfileVersionService.findLatestProfile(10001L);

        assertThat(result).isEmpty();
    }

    private UserProfileVersionCreateCommand command(Integer versionNo) {
        return new UserProfileVersionCreateCommand(
                10001L,
                versionNo,
                "{\"preferenceScore\":0.82,\"fulfilledScore\":0.91,\"complementScore\":0.78}",
                "User recently bought a keyboard and may need keyboard accessories.",
                "workflow-001"
        );
    }

    private UserProfileVersionEntity profileVersion(Integer versionNo) {
        UserProfileVersionEntity profileVersion = new UserProfileVersionEntity();
        profileVersion.setId(versionNo.longValue());
        profileVersion.setUserId(10001L);
        profileVersion.setVersionNo(versionNo);
        profileVersion.setProfileJson("{\"version\":" + versionNo + "}");
        profileVersion.setSummary("profile " + versionNo);
        profileVersion.setSourceWorkflowId("workflow-001");
        profileVersion.setCreatedAt(LocalDateTime.of(2026, 6, 29, 10, versionNo));
        return profileVersion;
    }
}
