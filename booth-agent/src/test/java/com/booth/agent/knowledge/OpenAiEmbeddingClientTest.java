package com.booth.agent.knowledge;

import com.booth.agent.config.BoothAgentProperties;
import com.booth.agent.support.Props;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiEmbeddingClientTest {

    @Test
    void embeddingsUrlAppendsV1Once() {
        assertEquals("https://api.openai.com/v1/embeddings",
                OpenAiEmbeddingClient.embeddingsUrl("https://api.openai.com"));
        assertEquals("https://api.openai.com/v1/embeddings",
                OpenAiEmbeddingClient.embeddingsUrl("https://api.openai.com/v1"));
    }

    @Test
    void readsFixedDimensionVector() throws Exception {
        String values = IntStream.range(0, 256)
                .mapToObj(i -> String.format(Locale.US, "%.1f", i == 0 ? 1.0 : 0.0))
                .collect(Collectors.joining(","));
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/embeddings", exchange -> {
            byte[] response = ("{\"data\":[{\"embedding\":[" + values + "]}]}")
                    .getBytes(StandardCharsets.UTF_8);
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
                            Props.rules().agent().confirmTtl(),
                            1,
                            0.72,
                            4,
                            2,
                            new BoothAgentProperties.Agent.Llm(true, "http://127.0.0.1:" + port, "sk-test", "gpt-4o-mini"),
                            new BoothAgentProperties.Agent.Embedding("openai", "text-embedding-3-small", 256),
                            BoothAgentProperties.Agent.Governance.defaults(),
                            BoothAgentProperties.Agent.Support.defaults(),
                            BoothAgentProperties.Agent.Skills.defaults()
                    ));
            float[] vector = new OpenAiEmbeddingClient(properties).embed("七天无理由");
            assertEquals(256, vector.length);
            assertTrue(Math.abs(vector[0] - 1.0f) < 0.0001f);
        } finally {
            server.stop(0);
        }
    }
}
