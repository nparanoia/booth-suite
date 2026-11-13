package com.booth.agent.llm;

import java.util.List;

public record ModelReply(String content, List<RequestedTool> toolCalls) {
    public ModelReply {
        toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
    }
}
