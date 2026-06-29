package com.personaflow.commerce.behavior.agent;

import com.personaflow.commerce.behavior.agent.dto.AgentProfileBuildResponse;
import com.personaflow.commerce.behavior.vo.AgentProfileContext;
import com.personaflow.commerce.common.error.BusinessException;
import com.personaflow.commerce.common.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HttpAgentProfileClientTest {

    @Test
    void buildProfilePostsAgentContextAndParsesResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpAgentProfileClient client = new HttpAgentProfileClient(properties(), builder, false);

        server.expect(requestTo("http://agent.test/agent/profile/build"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("\"userId\":10001")))
                .andRespond(withSuccess(profileResponseJson(), MediaType.APPLICATION_JSON));

        AgentProfileBuildResponse response = client.buildProfile(context());

        assertThat(response.workflowId()).isEqualTo("workflow-001");
        assertThat(response.profile().userId()).isEqualTo(10001L);
        assertThat(response.profile().summary()).contains("keyboard");
        assertThat(response.profile().profile()).containsEntry("profileSummary", "Keyboard buyer");
        server.verify();
    }

    @Test
    void buildProfileConvertsHttpFailureToProfileBuildFailed() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpAgentProfileClient client = new HttpAgentProfileClient(properties(), builder, false);

        server.expect(requestTo("http://agent.test/agent/profile/build"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.buildProfile(context()))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.AGENT_PROFILE_BUILD_FAILED));
        server.verify();
    }

    @Test
    void buildProfileConvertsConnectionFailureToServiceUnavailable() {
        RestClient.Builder builder = RestClient.builder()
                .requestFactory((uri, httpMethod) -> {
                    throw new IOException("agent is down");
                });
        HttpAgentProfileClient client = new HttpAgentProfileClient(properties(), builder, false);

        assertThatThrownBy(() -> client.buildProfile(context()))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.AGENT_SERVICE_UNAVAILABLE));
    }

    private AgentProperties properties() {
        AgentProperties properties = new AgentProperties();
        properties.setBaseUrl("http://agent.test");
        properties.setProfileBuildPath("/agent/profile/build");
        properties.setTimeoutMs(5000);
        return properties;
    }

    private AgentProfileContext context() {
        return new AgentProfileContext(
                10001L,
                List.of(),
                Map.of("PAYMENT_SUCCESS", 1L),
                List.of("keyboard"),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of("event-paid"),
                List.of(),
                LocalDateTime.of(2026, 6, 29, 12, 0)
        );
    }

    private String profileResponseJson() {
        return """
                {
                  "workflowId": "workflow-001",
                  "behaviorFactReport": {},
                  "intentReport": {},
                  "trendReport": {},
                  "profileDraft": {},
                  "auditReport": {},
                  "profile": {
                    "artifactId": "profile-001",
                    "workflowId": "workflow-001",
                    "userId": 10001,
                    "artifactType": "UserProfileVersion",
                    "createdAt": "2026-06-29T12:00:00Z",
                    "confidence": 0.82,
                    "evidenceEventIds": ["event-paid"],
                    "versionNo": 1,
                    "summary": "User recently bought a keyboard.",
                    "profile": {
                      "profileSummary": "Keyboard buyer",
                      "doNotRecommend": [{"skuId": 30001}]
                    }
                  }
                }
                """;
    }
}
