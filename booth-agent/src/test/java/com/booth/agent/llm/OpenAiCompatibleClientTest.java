package com.booth.agent.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.booth.agent.config.BoothAgentProperties;
import com.booth.agent.support.Props;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiCompatibleClientTest {

    @Test
    void completionsUrlAppendsV1Once() {
        assertEquals("https://api.openai.com/v1/chat/completions",
                OpenAiCompatibleClient.completionsUrl("https://api.openai.com"));
        assertEquals("https://api.openai.com/v1/chat/completions",
                OpenAiCompatibleClient.completionsUrl("https://api.openai.com/v1"));
        assertEquals("http://127.0.0.1:9/v1/chat/completions",
                OpenAiCompatibleClient.completionsUrl("http://127.0.0.1:9/"));
    }

    @Test
    void parsesToolCallAndSendsBearer() throws Exception {
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = """
                    {"choices":[{"message":{"role":"assistant","content":null,"tool_calls":[{"id":"call_1","type":"function","function":{"name":"query_order","arguments":"{\\"orderSn\\":\\"202610301234567890\\"}"}}]}}]}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            int port = server.getAddress().getPort();
            BoothAgentProperties properties = new BoothAgentProperties(
                    "http://127.0.0.1:9",
                    new BoothAgentProperties.Agent(
                            Props.llm().agent().confirmTtl(),
                            1,
                            Props.llm().agent().vectorMinScore(),
                            4,
                            2,
                            new BoothAgentProperties.Agent.Llm(true, "http://127.0.0.1:" + port, "sk-test", "gpt-4o-mini"),
                            Props.llm().agent().embedding(),
                            BoothAgentProperties.Agent.Governance.defaults(),
                            BoothAgentProperties.Agent.Support.defaults(),
                            BoothAgentProperties.Agent.Skills.defaults()
                    ));
            OpenAiCompatibleClient client = new OpenAiCompatibleClient(new ObjectMapper(), properties);
            ModelReply reply = client.complete(
                    List.of(ModelMessage.user("查订单")),
                    List.of(Map.of("type", "function", "function", Map.of("name", "query_order"))));
            assertEquals("Bearer sk-test", authorization.get());
            assertTrue(body.get().contains("query_order"));
            assertEquals(1, reply.toolCalls().size());
            assertEquals("query_order", reply.toolCalls().get(0).name());
            assertTrue(reply.toolCalls().get(0).argumentsJson().contains("202610301234567890"));
        } finally {
            server.stop(0);
        }
    }
}
