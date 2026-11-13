package com.booth.agent.ops;

import com.fasterxml.jackson.databind.JsonNode;
import com.booth.agent.common.ApiException;
import com.booth.agent.common.Tokens;
import com.booth.agent.config.BoothAgentProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * 运营接口只认 booth-cloud 管理员 Token，避免任何人匿名拉客服统计。
 */
@Service
public class OpsAuthService {

    private final RestClient restClient;

    public OpsAuthService(RestClient.Builder restClientBuilder, BoothAgentProperties properties) {
        this.restClient = restClientBuilder
                .baseUrl(properties.gatewayBaseUrl())
                .build();
    }

    public void requireAdmin(String authorization) {
        String token = Tokens.bearer(authorization);
        try {
            JsonNode body = restClient.get()
                    .uri("/user/admin/auth/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(JsonNode.class);
            if (body == null || body.path("code").asInt(-1) != 0 || body.path("data").isMissingNode()) {
                throw new ApiException(403, 40301, "需要有效的管理员登录");
            }
        } catch (ApiException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            throw new ApiException(403, 40301, "管理员鉴权失败");
        } catch (RuntimeException exception) {
            throw new ApiException(503, 50301, "无法校验管理员身份，请确认商城网关可用");
        }
    }
}
