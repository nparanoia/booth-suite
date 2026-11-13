package com.booth.agent.llm;

import java.util.List;

public record ModelMessage(
        String role,
        String content,
        String toolCallId,
        List<RequestedTool> toolCalls
) {
    public static ModelMessage system(String content) {
        return new ModelMessage("system", content, null, List.of());
    }

    public static ModelMessage user(String content) {
        return new ModelMessage("user", content, null, List.of());
    }

    public static ModelMessage assistant(String content, List<RequestedTool> toolCalls) {
        return new ModelMessage("assistant", content, null, toolCalls == null ? List.of() : List.copyOf(toolCalls));
    }

    public static ModelMessage tool(String toolCallId, String content) {
        return new ModelMessage("tool", content, toolCallId, List.of());
    }
}
