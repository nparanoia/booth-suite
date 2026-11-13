package com.booth.agent.trace;

import java.time.Instant;
import java.util.List;

/** 一轮对话的轨迹。参数摘要里没有令牌原文和完整手机号。 */
public record TurnTrace(
        String conversationId,
        String member,
        String intent,
        boolean retrievalHit,
        List<String> tools,
        String argumentSummary,
        long elapsedMs,
        String errorCode,
        Instant at
) {
    public TurnTrace {
        tools = tools == null ? List.of() : List.copyOf(tools);
        if (argumentSummary == null) {
            argumentSummary = "";
        }
    }
}
