package com.booth.agent.knowledge;

import com.fasterxml.jackson.databind.JsonNode;
import com.booth.agent.config.BoothAgentProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/** OpenAI 兼容 embeddings。请求维度固定 256，和本地表一致。 */
public final class OpenAiEmbeddingClient implements TextEmbedder {

    private final RestClient restClient;
    private final BoothAgentProperties properties;

    public OpenAiEmbeddingClient(BoothAgentProperties properties) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
        factory.setReadTimeout(Duration.ofSeconds(60));
        this.restClient = RestClient.builder().requestFactory(factory).build();
        this.properties = properties;
    }

    @Override
    public String name() {
        BoothAgentProperties.Agent.Embedding embedding = properties.agent().embedding();
        return "openai:" + (embedding == null || embedding.model() == null ? "unknown" : embedding.model());
    }

    @Override
    public float[] embed(String text) {
        BoothAgentProperties.Agent.Llm llm = properties.agent().llm();
        BoothAgentProperties.Agent.Embedding embedding = properties.agent().embedding();
        if (llm == null || !StringUtils.hasText(llm.apiKey())) {
            throw new IllegalStateException("未配置 embedding 密钥");
        }
        String model = embedding == null || !StringUtils.hasText(embedding.model())
                ? "text-embedding-3-small" : embedding.model().trim();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("input", text == null ? "" : text);
        body.put("dimensions", TextEmbedder.DIMENSION);
        JsonNode root = restClient.post()
                .uri(embeddingsUrl(llm.baseUrl()))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + llm.apiKey().trim())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);
        JsonNode values = root == null ? null : root.path("data").path(0).path("embedding");
        if (values == null || !values.isArray() || values.isEmpty()) {
            throw new IllegalStateException("embedding 返回为空");
        }
        float[] vector = new float[TextEmbedder.DIMENSION];
        int length = Math.min(TextEmbedder.DIMENSION, values.size());
        for (int i = 0; i < length; i++) {
            vector[i] = (float) values.get(i).asDouble();
        }
        normalize(vector);
        return vector;
    }

    static String embeddingsUrl(String baseUrl) {
        String base = baseUrl == null ? "https://api.openai.com" : baseUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (base.endsWith("/v1")) {
            return base + "/embeddings";
        }
        return base + "/v1/embeddings";
    }

    private static void normalize(float[] vector) {
        double norm = 0;
        for (float value : vector) {
            norm += value * value;
        }
        norm = Math.sqrt(norm);
        if (norm == 0) {
            return;
        }
        for (int i = 0; i < vector.length; i++) {
            vector[i] = (float) (vector[i] / norm);
        }
    }
}
