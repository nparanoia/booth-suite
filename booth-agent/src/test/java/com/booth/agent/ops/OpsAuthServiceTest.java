package com.booth.agent.ops;

import com.booth.agent.common.ApiException;
import com.booth.agent.config.BoothAgentProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpsAuthServiceTest {

    @Test
    void acceptsValidAdmin() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://gateway.test/user/admin/auth/me"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer adm-token"))
                .andRespond(withSuccess("{\"code\":0,\"message\":\"ok\",\"data\":{\"id\":1,\"username\":\"admin\"}}",
                        MediaType.APPLICATION_JSON));

        OpsAuthService service = new OpsAuthService(builder, props());
        service.requireAdmin("Bearer adm-token");
        server.verify();
    }

    @Test
    void rejectsMissingToken() {
        OpsAuthService service = new OpsAuthService(RestClient.builder(), props());
        ApiException ex = assertThrows(ApiException.class, () -> service.requireAdmin(null));
        assertEquals(401, ex.getHttpStatus());
    }

    @Test
    void rejectsFailedGatewayCode() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://gateway.test/user/admin/auth/me"))
                .andRespond(withSuccess("{\"code\":40100,\"message\":\"未登录\",\"data\":null}", MediaType.APPLICATION_JSON));

        OpsAuthService service = new OpsAuthService(builder, props());
        ApiException ex = assertThrows(ApiException.class, () -> service.requireAdmin("Bearer bad"));
        assertEquals(403, ex.getHttpStatus());
        server.verify();
    }

    private static BoothAgentProperties props() {
        return new BoothAgentProperties(
                "http://gateway.test",
                new BoothAgentProperties.Agent(
                        java.time.Duration.ofMinutes(10),
                        1,
                        0.72,
                        4,
                        2,
                        new BoothAgentProperties.Agent.Llm(false, "http://x", "", "m"),
                        new BoothAgentProperties.Agent.Embedding("ngram", "n", 256),
                        BoothAgentProperties.Agent.Governance.defaults(),
                        BoothAgentProperties.Agent.Support.defaults(),
                        BoothAgentProperties.Agent.Skills.defaults()));
    }
}
