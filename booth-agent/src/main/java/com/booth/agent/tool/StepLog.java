package com.booth.agent.tool;

import com.booth.agent.chat.ChatResponse;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record StepLog(
        String name,
        Map<String, Object> arguments,
        int attempts,
        boolean success,
        String errorKind,
        boolean retried,
        boolean terminal,
        String reply,
        ChatResponse.ConfirmCard confirm,
        List<String> sources
) {
    public StepLog {
        arguments = arguments == null ? Map.of() : new LinkedHashMap<>(arguments);
        sources = sources == null ? List.of() : List.copyOf(sources);
    }

    public static StepLog ok(String name, Map<String, Object> arguments, int attempts, ToolResult result) {
        return new StepLog(name, arguments, attempts, true, null, attempts > 1, false,
                result.reply(), result.confirm(), result.sources());
    }

    public static StepLog stop(String name, Map<String, Object> arguments, String errorKind,
                               int attempts, boolean retried, boolean terminal, String reply) {
        return new StepLog(name, arguments, attempts, false, errorKind, retried, terminal,
                reply, null, List.of());
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", name);
        map.put("arguments", new LinkedHashMap<>(arguments));
        map.put("attempts", attempts);
        map.put("success", success);
        map.put("errorKind", errorKind);
        map.put("retried", retried);
        map.put("message", reply);
        return map;
    }

    public static List<StepLog> one(StepLog step) {
        List<StepLog> steps = new ArrayList<>();
        steps.add(step);
        return steps;
    }
}
