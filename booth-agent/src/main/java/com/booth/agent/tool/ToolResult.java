package com.booth.agent.tool;

import com.booth.agent.chat.ChatResponse;

import java.util.List;

public record ToolResult(
        String reply,
        ChatResponse.ConfirmCard confirm,
        List<String> sources
) {
    public ToolResult {
        sources = sources == null ? List.of() : List.copyOf(sources);
    }
}
