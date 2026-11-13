package com.booth.agent.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.booth.agent.common.ToolErrorKind;
import com.booth.agent.common.ToolExecutionException;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.net.ConnectException;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StoreErrorsTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void businessRejectionKeepsStoreMessage() throws Exception {
        ToolExecutionException error = assertThrows(ToolExecutionException.class, () -> StoreErrors.unwrap(
                mapper.readTree("{\"code\":30201,\"message\":\"订单不存在\",\"data\":null}")));
        assertEquals(ToolErrorKind.REJECTED, error.kind());
        assertEquals(30201, error.code());
        assertEquals("订单不存在", error.getMessage());
    }

    @Test
    void successReturnsData() throws Exception {
        var data = StoreErrors.unwrap(mapper.readTree("{\"code\":0,\"message\":\"ok\",\"data\":{\"orderSn\":\"1\"}}"));
        assertEquals("1", data.get("orderSn").asText());
    }

    @Test
    void stackIsNotForwarded() throws Exception {
        ToolExecutionException error = assertThrows(ToolExecutionException.class, () -> StoreErrors.unwrap(mapper.readTree(
                "{\"code\":500,\"message\":\"java.lang.IllegalStateException at com.booth.Foo\"}")));
        assertEquals("商城调用失败", error.getMessage());
        assertFalse(error.getMessage().contains("Exception"));
    }

    @Test
    void http401StopsAsAuth() {
        ToolExecutionException error = StoreErrors.response(http(401, "{\"message\":\"java.lang.Exception\"}"));
        assertEquals(ToolErrorKind.AUTH, error.kind());
        assertEquals("未登录或登录已过期", error.getMessage());
    }

    @Test
    void tooManyRequestsIsNotRetriedAsTimeout() {
        ToolExecutionException error = StoreErrors.response(http(429, "{\"message\":\"请求过于频繁\"}"));
        assertEquals(ToolErrorKind.REJECTED, error.kind());
        assertEquals("请求过于频繁", error.getMessage());
    }

    @Test
    void readTimeoutIsTimeout() {
        ResourceAccessException access = new ResourceAccessException("I/O", new HttpTimeoutException("request timed out"));
        assertTrue(StoreErrors.timeout(access));
        ToolExecutionException error = StoreErrors.access(access);
        assertEquals(ToolErrorKind.TIMEOUT, error.kind());
        assertEquals("商城响应超时", error.getMessage());
    }

    @Test
    void connectionRefusedIsNotTimeout() {
        ToolExecutionException error = StoreErrors.access(
                new ResourceAccessException("I/O", new ConnectException("Connection refused")));
        assertEquals(ToolErrorKind.REJECTED, error.kind());
        assertEquals("商城暂时不可用", error.getMessage());
        assertFalse(StoreErrors.timeout(error));
    }

    private static RestClientResponseException http(int status, String body) {
        return new RestClientResponseException("status", status, "status", null,
                body.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }
}
