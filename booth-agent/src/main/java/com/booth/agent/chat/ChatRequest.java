package com.booth.agent.chat;

import jakarta.validation.constraints.Size;

/**
 * message 与 contextOrderSn 至少有一个；入口带单时可只传 contextOrderSn，由服务端校验后开场。
 */
public record ChatRequest(
        @Size(max = 1000) String message,
        @Size(max = 64) String conversationId,
        @Size(max = 64) String contextOrderSn,
        @Size(max = 32) String contextHint
) {
    public ChatRequest(String message, String conversationId) {
        this(message, conversationId, null, null);
    }

    public boolean hasContextOrderSn() {
        return contextOrderSn != null && !contextOrderSn.isBlank();
    }

    public boolean hasUserMessage() {
        return message != null && !message.isBlank();
    }
}
