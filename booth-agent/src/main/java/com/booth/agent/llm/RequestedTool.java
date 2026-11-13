package com.booth.agent.llm;

public record RequestedTool(String id, String name, String argumentsJson) {
}
