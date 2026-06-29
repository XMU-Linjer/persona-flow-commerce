package com.personaflow.commerce.behavior.dto;

public record UserProfileVersionCreateCommand(
        Long userId,
        Integer versionNo,
        String profileJson,
        String summary,
        String sourceWorkflowId
) {
}
