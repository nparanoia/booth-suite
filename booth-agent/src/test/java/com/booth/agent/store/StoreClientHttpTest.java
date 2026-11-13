package com.booth.agent.store;

import com.fasterxml.jackson.databind.JsonNode;
import com.booth.agent.common.ToolErrorKind;
import com.booth.agent.common.ToolExecutionException;
import com.booth.agent.config.BoothAgentProperties;
import com.booth.agent.support.Props;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StoreClientHttpTest {

    private HttpServer server;
    private StoreClient client;
    private final AtomicReference<String> authorization = new AtomicReference<>();

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/trade", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            String path = exchange.getRequestURI().getPath();
            int status = 200;
            String body;
            if (path.endsWith("202610309999999999")) {
                body = "{\"code\":30201,\"message\":\"订单不存在\",\"data\":null}";
            } else if (path.endsWith("202610301111111111")) {
                status = 401;
                body = "{\"code\":401,\"message\":\"java.lang.Exception\"}";
            } else {
                body = "{\"code\":0,\"message\":\"ok\",\"data\":{\"orderSn\":\"202610301234567890\",\"status\":\"COMPLETED\",\"payAmount\":\"12.34\"}}";
            }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        int port = server.getAddress().getPort();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
        factory.setReadTimeout(Duration.ofSeconds(2));
        BoothAgentProperties properties = new BoothAgentProperties(
                "http://127.0.0.1:" + port,
                Props.rules().agent());
        client = new StoreClient(RestClient.builder().requestFactory(factory), properties);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void ownOrderUsesBearerAndUnwrapsData() {
        JsonNode data = client.getOrder("member-a", "202610301234567890");
        assertEquals("Bearer member-a", authorization.get());
        assertEquals("COMPLETED", data.get("status").asText());
        assertEquals("12.34", data.get("payAmount").asText());
    }

    @Test
    void otherOrderIsBusinessRejection() {
        ToolExecutionException error = assertThrows(ToolExecutionException.class,
                () -> client.getOrder("member-b", "202610309999999999"));
        assertEquals(ToolErrorKind.REJECTED, error.kind());
        assertEquals("订单不存在", error.getMessage());
    }

    @Test
    void gateway401DoesNotLeakBody() {
        ToolExecutionException error = assertThrows(ToolExecutionException.class,
                () -> client.getOrder("member-a", "202610301111111111"));
        assertEquals(ToolErrorKind.AUTH, error.kind());
        assertEquals("未登录或登录已过期", error.getMessage());
        assertFalse(error.getMessage().contains("Exception"));
    }
}
