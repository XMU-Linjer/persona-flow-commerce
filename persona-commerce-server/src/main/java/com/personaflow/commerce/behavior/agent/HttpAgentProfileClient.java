package com.personaflow.commerce.behavior.agent;

import com.personaflow.commerce.behavior.agent.dto.AgentProfileBuildRequest;
import com.personaflow.commerce.behavior.agent.dto.AgentProfileBuildResponse;
import com.personaflow.commerce.behavior.vo.AgentProfileContext;
import com.personaflow.commerce.common.error.BusinessException;
import com.personaflow.commerce.common.error.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class HttpAgentProfileClient implements AgentProfileClient {

    private static final Logger log = LoggerFactory.getLogger(HttpAgentProfileClient.class);
    private static final String REQUEST_ID_HEADER = "X-Profile-Request-Id";

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
        String requestId = UUID.randomUUID().toString();
        long startedAt = System.nanoTime();
        int eventCount = context.recentEvents() == null ? 0 : context.recentEvents().size();
        log.info(
                "Agent profile request started requestId={}, userId={}, eventCount={}, timeoutMs={}",
                requestId,
                context.userId(),
                eventCount,
                properties.getTimeoutMs()
        );
        try {
            AgentProfileBuildResponse response = restClient.post()
                    .uri(normalizePath(properties.getProfileBuildPath()))
                    .header(REQUEST_ID_HEADER, requestId)
                    .body(AgentProfileBuildRequest.from(context))
                    .retrieve()
                    .body(AgentProfileBuildResponse.class);
            if (response == null || response.profile() == null || response.profile().profile() == null) {
                log.warn(
                        "Agent profile response invalid requestId={}, userId={}, elapsedMs={}",
                        requestId,
                        context.userId(),
                        elapsedMillis(startedAt)
                );
                throw new BusinessException(ErrorCode.AGENT_PROFILE_BUILD_FAILED);
            }
            log.info(
                    "Agent profile request succeeded requestId={}, userId={}, workflowId={}, generationMode={}, elapsedMs={}",
                    requestId,
                    context.userId(),
                    response.workflowId(),
                    generationMode(response.profile().profile()),
                    elapsedMillis(startedAt)
            );
            return response;
        } catch (BusinessException exception) {
            throw exception;
        } catch (ResourceAccessException exception) {
            log.warn(
                    "Agent profile request unavailable requestId={}, userId={}, elapsedMs={}, errorType={}, reason={}",
                    requestId,
                    context.userId(),
                    elapsedMillis(startedAt),
                    exception.getClass().getSimpleName(),
                    safeReason(exception)
            );
            throw new BusinessException(ErrorCode.AGENT_SERVICE_UNAVAILABLE);
        } catch (RestClientResponseException exception) {
            log.warn(
                    "Agent profile request returned error requestId={}, userId={}, status={}, elapsedMs={}, response={}",
                    requestId,
                    context.userId(),
                    exception.getStatusCode().value(),
                    elapsedMillis(startedAt),
                    safeResponseBody(exception.getResponseBodyAsString())
            );
            throw new BusinessException(ErrorCode.AGENT_PROFILE_BUILD_FAILED);
        } catch (RestClientException exception) {
            log.warn(
                    "Agent profile request failed requestId={}, userId={}, elapsedMs={}, errorType={}, reason={}",
                    requestId,
                    context.userId(),
                    elapsedMillis(startedAt),
                    exception.getClass().getSimpleName(),
                    safeReason(exception)
            );
            throw new BusinessException(ErrorCode.AGENT_PROFILE_BUILD_FAILED);
        }
    }

    private static long elapsedMillis(long startedAt) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }

    private static Object generationMode(Map<String, Object> profile) {
        return profile.getOrDefault("generationMode", "unknown");
    }

    private static String safeReason(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return truncate(message.replaceAll("(?i)(bearer\\s+)[^\\s,;]+", "$1[redacted]"), 300);
    }

    private static String safeResponseBody(String body) {
        if (body == null || body.isBlank()) {
            return "<empty>";
        }
        return truncate(body.replaceAll("(?i)(api[_-]?key|authorization|token)\\s*[:=]\\s*[^,}\\s]+", "$1=[redacted]"), 500);
    }

    private static String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength) + "...";
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
