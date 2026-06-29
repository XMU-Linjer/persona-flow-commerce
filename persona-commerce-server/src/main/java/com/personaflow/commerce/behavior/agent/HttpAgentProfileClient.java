package com.personaflow.commerce.behavior.agent;

import com.personaflow.commerce.behavior.agent.dto.AgentProfileBuildRequest;
import com.personaflow.commerce.behavior.agent.dto.AgentProfileBuildResponse;
import com.personaflow.commerce.behavior.vo.AgentProfileContext;
import com.personaflow.commerce.common.error.BusinessException;
import com.personaflow.commerce.common.error.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;

@Component
public class HttpAgentProfileClient implements AgentProfileClient {

    private final AgentProperties properties;
    private final RestClient restClient;

    @Autowired
    public HttpAgentProfileClient(AgentProperties properties, RestClient.Builder restClientBuilder) {
        this(properties, restClientBuilder, true);
    }

    HttpAgentProfileClient(
            AgentProperties properties,
            RestClient.Builder restClientBuilder,
            boolean configureTimeout
    ) {
        this.properties = properties;
        RestClient.Builder builder = restClientBuilder.baseUrl(stripTrailingSlash(properties.getBaseUrl()));
        if (configureTimeout) {
            builder.requestFactory(requestFactory(properties.getTimeoutMs()));
        }
        this.restClient = builder.build();
    }

    @Override
    public AgentProfileBuildResponse buildProfile(AgentProfileContext context) {
        try {
            AgentProfileBuildResponse response = restClient.post()
                    .uri(normalizePath(properties.getProfileBuildPath()))
                    .body(AgentProfileBuildRequest.from(context))
                    .retrieve()
                    .body(AgentProfileBuildResponse.class);
            if (response == null || response.profile() == null || response.profile().profile() == null) {
                throw new BusinessException(ErrorCode.AGENT_PROFILE_BUILD_FAILED);
            }
            return response;
        } catch (BusinessException exception) {
            throw exception;
        } catch (ResourceAccessException exception) {
            throw new BusinessException(ErrorCode.AGENT_SERVICE_UNAVAILABLE);
        } catch (RestClientResponseException exception) {
            throw new BusinessException(ErrorCode.AGENT_PROFILE_BUILD_FAILED);
        } catch (RestClientException exception) {
            throw new BusinessException(ErrorCode.AGENT_PROFILE_BUILD_FAILED);
        }
    }

    private static SimpleClientHttpRequestFactory requestFactory(long timeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        Duration timeout = Duration.ofMillis(timeoutMs);
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        return requestFactory;
    }

    private static String stripTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://127.0.0.1:8001";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private static String normalizePath(String value) {
        if (value == null || value.isBlank()) {
            return "/agent/profile/build";
        }
        return value.startsWith("/") ? value : "/" + value;
    }
}
