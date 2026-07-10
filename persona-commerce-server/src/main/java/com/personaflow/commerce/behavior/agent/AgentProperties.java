package com.personaflow.commerce.behavior.agent;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "persona.agent")
public class AgentProperties {

    private String baseUrl = "http://127.0.0.1:8001";
    private String profileBuildPath = "/agent/profile/build";
    private long timeoutMs = 30000;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getProfileBuildPath() {
        return profileBuildPath;
    }

    public void setProfileBuildPath(String profileBuildPath) {
        this.profileBuildPath = profileBuildPath;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
}
