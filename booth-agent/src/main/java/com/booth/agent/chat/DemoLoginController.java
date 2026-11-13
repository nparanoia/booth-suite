package com.booth.agent.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.booth.agent.common.ApiException;
import com.booth.agent.common.Result;
import com.booth.agent.config.BoothAgentProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 演示页登录代理，避免浏览器跨域打网关。
 * 不接受客户端自定义 gateway（防 SSRF）；同一 IP 短时间失败次数有上限。
 */
@Tag(name = "演示")
@RestController
@RequestMapping("/app/demo")
public class DemoLoginController {

    private static final int WINDOW_MS = 60_000;
    private static final int MAX_FAILS = 20;

    private final RestClient restClient;
    private final ConcurrentHashMap<String, Window> fails = new ConcurrentHashMap<>();

    public DemoLoginController(RestClient.Builder restClientBuilder, BoothAgentProperties properties) {
        this.restClient = restClientBuilder.baseUrl(properties.gatewayBaseUrl()).build();
    }

    @Operation(summary = "演示登录", description = "只转发到配置的商城网关会员登录，返回 token")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody DemoLoginRequest request,
                                             HttpServletRequest httpRequest) {
        String ip = clientIp(httpRequest);
        guard(ip);
        try {
            JsonNode body = restClient.post()
                    .uri("/user/open/members/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("username", request.username(), "password", request.password()))
                    .retrieve()
                    .body(JsonNode.class);
            if (body == null || body.path("code").asInt(-1) != 0) {
                recordFail(ip);
                String message = body == null ? "登录失败" : body.path("message").asText("登录失败");
                throw new ApiException(401, body == null ? 40100 : body.path("code").asInt(40100), message);
            }
            String token = body.path("data").path("token").asText();
            if (token == null || token.isBlank()) {
                recordFail(ip);
                throw new ApiException(401, 40100, "登录成功但没有 token");
            }
            fails.remove(ip);
            return Result.ok(Map.of("token", token, "username", request.username()));
        } catch (ApiException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            recordFail(ip);
            throw new ApiException(exception.getStatusCode().value(), 40100, "商城登录失败");
        }
    }

    private void guard(String ip) {
        Window window = fails.get(ip);
        if (window == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - window.startedAt > WINDOW_MS) {
            fails.remove(ip);
            return;
        }
        if (window.count.get() >= MAX_FAILS) {
            throw new ApiException(429, 42900, "登录尝试过多，请稍后再试");
        }
    }

    private void recordFail(String ip) {
        long now = System.currentTimeMillis();
        fails.compute(ip, (key, old) -> {
            if (old == null || now - old.startedAt > WINDOW_MS) {
                return new Window(now, new AtomicInteger(1));
            }
            old.count.incrementAndGet();
            return old;
        });
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String[] parts = forwarded.split(",");
            return parts[parts.length - 1].trim();
        }
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }

    public record DemoLoginRequest(
            @NotBlank @Size(max = 64) String username,
            @NotBlank @Size(max = 32) String password
    ) {
    }

    private record Window(long startedAt, AtomicInteger count) {
    }
}
