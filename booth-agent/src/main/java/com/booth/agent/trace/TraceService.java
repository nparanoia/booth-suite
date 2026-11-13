package com.booth.agent.trace;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.booth.agent.chat.ChatResponse;
import com.booth.agent.common.Tokens;
import com.booth.agent.confirm.ConfirmStore;
import com.booth.agent.ops.OpsMetrics;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class TraceService {

    private static final String PREFIX = "booth:agent:trace:";
    private static final Duration TTL = Duration.ofDays(7);
    private static final int MAX_TURNS = 50;
    private static final TypeReference<List<TurnTrace>> LIST = new TypeReference<>() {
    };

    private final ConfirmStore store;
    private final ObjectMapper objectMapper;
    private final OpsMetrics opsMetrics;

    public TraceService(ConfirmStore store, ObjectMapper objectMapper, OpsMetrics opsMetrics) {
        this.store = store;
        this.objectMapper = objectMapper;
        this.opsMetrics = opsMetrics;
    }

    public void recordChat(String memberToken, ChatResponse response, long startedNanos) {
        if (response == null || response.conversationId() == null || response.conversationId().isBlank()) {
            return;
        }
        List<String> tools = new ArrayList<>();
        StringBuilder arguments = new StringBuilder();
        Object calls = response.debug() == null ? null : response.debug().get("toolCalls");
        if (calls instanceof List<?> list) {
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> map)) {
                    continue;
                }
                Object name = map.get("name");
                if (name != null) {
                    tools.add(name.toString());
                }
                if (!arguments.isEmpty()) {
                    arguments.append("; ");
                }
                arguments.append(TraceMask.summarize(map.get("arguments")));
            }
        }
        String stopped = response.debug() == null ? null : string(response.debug().get("stoppedReason"));
        String error = toolError(calls);
        if (stopped != null && !"CONFIRM_REQUIRED".equals(stopped)) {
            error = stopped;
        }
        boolean hit = response.sources() != null && !response.sources().isEmpty();
        append(memberToken, new TurnTrace(
                response.conversationId(),
                Tokens.fingerprint(memberToken),
                response.intent(),
                hit,
                tools,
                clip(arguments.toString()),
                elapsed(startedNanos),
                error,
                Instant.now()));
    }

    public void recordConfirm(String memberToken, String conversationId, String argumentSummary,
                              boolean success, String errorCode, long startedNanos) {
        String id = conversationId == null || conversationId.isBlank() ? "unknown" : conversationId;
        String tool;
        String intent;
        if (argumentSummary != null && argumentSummary.contains("action=cancel")) {
            tool = "cancel_order";
            intent = "CANCEL_ORDER";
        } else if (argumentSummary != null && argumentSummary.contains("action=change_address")) {
            tool = "change_address";
            intent = "CHANGE_ADDRESS";
        } else {
            tool = "apply_return";
            intent = "AFTER_SALE";
        }
        append(memberToken, new TurnTrace(
                id,
                Tokens.fingerprint(memberToken),
                intent,
                false,
                List.of(tool),
                clip(TraceMask.maskText(argumentSummary)),
                elapsed(startedNanos),
                success ? null : errorCode,
                Instant.now()));
    }

    public List<TurnTrace> find(String memberToken, String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return List.of();
        }
        String json = store.get(key(memberToken, conversationId));
        if (json == null) {
            return List.of();
        }
        try {
            List<TurnTrace> traces = objectMapper.readValue(json, LIST);
            return traces == null ? List.of() : List.copyOf(traces);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void append(String memberToken, TurnTrace trace) {
        List<TurnTrace> traces = new ArrayList<>(find(memberToken, trace.conversationId()));
        traces.add(trace);
        if (traces.size() > MAX_TURNS) {
            traces = new ArrayList<>(traces.subList(traces.size() - MAX_TURNS, traces.size()));
        }
        try {
            store.save(key(memberToken, trace.conversationId()), objectMapper.writeValueAsString(traces), TTL);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(exception);
        }
        opsMetrics.observe(trace);
    }

    private static String key(String memberToken, String conversationId) {
        return PREFIX + Tokens.fingerprint(memberToken) + ":" + conversationId;
    }

    private static long elapsed(long startedNanos) {
        return Math.max(0, (System.nanoTime() - startedNanos) / 1_000_000);
    }

    private static String toolError(Object calls) {
        if (!(calls instanceof List<?> list)) {
            return null;
        }
        String error = null;
        for (Object item : list) {
            if (item instanceof Map<?, ?> map && map.get("errorKind") != null) {
                error = map.get("errorKind").toString();
            }
        }
        return error;
    }

    private static String string(Object value) {
        return value == null ? null : value.toString();
    }

    private static String clip(String text) {
        if (text == null) {
            return "";
        }
        return text.length() <= 300 ? text : text.substring(0, 300);
    }
}
