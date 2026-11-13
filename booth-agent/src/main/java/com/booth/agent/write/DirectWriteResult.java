package com.booth.agent.write;

/** 对话内直写结果（供工具层拼回复）。 */
public record DirectWriteResult(
        String reply,
        boolean idempotentReplay,
        String resultRef,
        long outboxId
) {
}
