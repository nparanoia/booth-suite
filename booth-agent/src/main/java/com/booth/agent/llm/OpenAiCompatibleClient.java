package com.booth.agent.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.booth.agent.config.BoothAgentProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** OpenAI 兼容 chat completions（单测与兼容保留；生产对话走 Spring AI ChatClient）。 */
@Slf4j
public class OpenAiCompatibleClient implements LlmClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final BoothAgentProperties properties;

    public OpenAiCompatibleClient(ObjectMapper objectMapper, BoothAgentProperties properties) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
        factory.setReadTimeout(Duration.ofSeconds(60));
        this.restClient = RestClient.builder().requestFactory(factory).build();
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public ModelReply complete(List<ModelMessage> messages, List<Map<String, Object>> tools) {
        BoothAgentProperties.Agent.Llm llm = properties.agent().llm();
        if (llm.apiKey() == null || llm.apiKey().isBlank()) {
            throw new LlmUnavailableException("未配置模型密钥");
        }
        if (!StringUtils.hasText(llm.model())) {
            throw new LlmUnavailableException("未配置模型名称");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", llm.model().trim());
        body.put("temperature", 0);
        body.put("messages", messages.stream().map(this::toMap).toList());
        if (tools != null && !tools.isEmpty()) {
            body.put("tools", tools);
            body.put("tool_choice", "auto");
        }
        try {
            JsonNode root = restClient.post()
                    .uri(completionsUrl(llm.baseUrl()))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + llm.apiKey().trim())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            return parse(root);
        } catch (RestClientResponseException exception) {
            log.warn("llm http status={} body={}", exception.getStatusCode().value(), preview(exception.getResponseBodyAsString()));
            throw new LlmUnavailableException("大模型接口返回 " + exception.getStatusCode().value());
        } catch (RestClientException exception) {
            log.warn("llm 调用失败: {}", exception.getClass().getSimpleName());
            throw new LlmUnavailableException("大模型接口不可用");
        }
    }

    static String completionsUrl(String baseUrl) {
        String base = baseUrl == null ? "" : baseUrl.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (base.endsWith("/v1")) {
            return base + "/chat/completions";
        }
        return base + "/v1/chat/completions";
    }

    private ModelReply parse(JsonNode root) {
        JsonNode message = root == null ? null : root.path("choices").path(0).path("message");
        if (message == null || message.isMissingNode() || message.isNull()) {
            throw new LlmUnavailableException("大模型返回无法识别");
        }
        String content = message.path("content").isTextual() ? message.path("content").asText() : null;
        List<RequestedTool> calls = new ArrayList<>();
        JsonNode toolCalls = message.path("tool_calls");
        if (toolCalls.isArray()) {
            int index = 0;
            for (JsonNode call : toolCalls) {
                JsonNode function = call.path("function");
                String id = call.path("id").asText("call-" + index);
                String name = function.path("name").asText("").trim();
                calls.add(new RequestedTool(id, name, argumentsJson(function.path("arguments"))));
                index++;
            }
        }
        return new ModelReply(content, calls);
    }

    private String argumentsJson(JsonNode arguments) {
        if (arguments == null || arguments.isMissingNode() || arguments.isNull()) {
            return "{}";
        }
        if (arguments.isTextual()) {
            return arguments.asText();
        }
        try {
            return objectMapper.writeValueAsString(arguments);
        } catch (Exception exception) {
            return "{}";
        }
    }

    private Map<String, Object> toMap(ModelMessage message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("role", message.role());
        if ("tool".equals(message.role())) {
            map.put("tool_call_id", message.toolCallId());
            map.put("content", message.content() == null ? "" : message.content());
            return map;
        }
        if ("assistant".equals(message.role()) && message.toolCalls() != null && !message.toolCalls().isEmpty()) {
            map.put("content", message.content() == null ? "" : message.content());
            List<Map<String, Object>> calls = new ArrayList<>();
            for (RequestedTool call : message.toolCalls()) {
                Map<String, Object> function = new LinkedHashMap<>();
                function.put("name", call.name());
                function.put("arguments", call.argumentsJson() == null ? "{}" : call.argumentsJson());
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", call.id());
                item.put("type", "function");
                item.put("function", function);
                calls.add(item);
            }
            map.put("tool_calls", calls);
            return map;
        }
        map.put("content", message.content() == null ? "" : message.content());
        return map;
    }

    private static String preview(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        String oneLine = body.replace('\n', ' ').replace('\r', ' ').trim();
        return oneLine.length() > 180 ? oneLine.substring(0, 180) : oneLine;
    }
}
