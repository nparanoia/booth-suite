package com.booth.agent.store;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.booth.agent.common.ToolErrorKind;
import com.booth.agent.common.ToolExecutionException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;

/** 把商城 HTTP / 业务码收成可重试或必须停止的错误。不把响应体里的堆栈交给模型。 */
final class StoreErrors {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private StoreErrors() {
    }

    static JsonNode unwrap(JsonNode root) {
        if (root == null || !root.isObject()) {
            throw ToolExecutionException.booth(ToolErrorKind.REJECTED, 502, "商城返回无法识别");
        }
        int code = root.path("code").asInt(-1);
        if (code == 0) {
            return root.get("data");
        }
        String message = safe(root.path("message").asText(null));
        if (code == 401 || code == 403) {
            throw ToolExecutionException.booth(ToolErrorKind.AUTH, code,
                    code == 401 ? "未登录或登录已过期" : "没有访问权限");
        }
        throw ToolExecutionException.booth(ToolErrorKind.REJECTED, code, message);
    }

    static ToolExecutionException response(RestClientResponseException exception) {
        int status = exception.getStatusCode().value();
        if (status == 401 || status == 403) {
            return ToolExecutionException.booth(ToolErrorKind.AUTH, status,
                    status == 401 ? "未登录或登录已过期" : "没有访问权限");
        }
        if (status == 408 || status == 504) {
            return ToolExecutionException.booth(ToolErrorKind.TIMEOUT, status, "商城响应超时");
        }
        String body = exception.getResponseBodyAsString(StandardCharsets.UTF_8);
        return ToolExecutionException.booth(ToolErrorKind.REJECTED, status, messageOf(body));
    }

    static ToolExecutionException access(ResourceAccessException exception) {
        if (timeout(exception)) {
            return ToolExecutionException.booth(ToolErrorKind.TIMEOUT, 504, "商城响应超时");
        }
        return ToolExecutionException.booth(ToolErrorKind.REJECTED, 502, "商城暂时不可用");
    }

    static boolean timeout(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof HttpTimeoutException) {
                return true;
            }
            String message = current.getMessage();
            if (message != null && message.toLowerCase().contains("timed out")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static String messageOf(String body) {
        if (body == null || body.isBlank()) {
            return "商城调用失败";
        }
        try {
            JsonNode node = MAPPER.readTree(body);
            if (node.isObject() && node.hasNonNull("message")) {
                return safe(node.get("message").asText());
            }
        } catch (Exception ignored) {
            // 不是 JSON 时走下面的纯文本过滤
        }
        return safe(body);
    }

    static String safe(String raw) {
        if (raw == null || raw.isBlank()) {
            return "商城调用失败";
        }
        String oneLine = raw.replace('\r', ' ').replace('\n', ' ').trim();
        String lower = oneLine.toLowerCase();
        if (lower.contains("exception") || oneLine.contains("\tat") || lower.contains("at com.") || lower.contains("at org.")) {
            return "商城调用失败";
        }
        if (oneLine.length() > 120) {
            return oneLine.substring(0, 120);
        }
        return oneLine;
    }
}
