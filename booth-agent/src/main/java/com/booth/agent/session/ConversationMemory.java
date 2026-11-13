package com.booth.agent.session;

import java.time.Instant;
import java.util.List;

public record ConversationMemory(
        String conversationId,
        String memberHash,
        ConversationStep step,
        String orderSn,
        String ticketId,
        Long orderItemId,
        String reason,
        String summary,
        List<StoredTurn> turns,
        Instant touchedAt
) {
    public ConversationMemory {
        turns = turns == null ? List.of() : List.copyOf(turns);
        if (step == null) {
            step = ConversationStep.CHAT;
        }
        if (summary != null && summary.isBlank()) {
            summary = null;
        }
        if (touchedAt == null) {
            touchedAt = Instant.now();
        }
    }

    public ConversationMemory clearedPending() {
        return new ConversationMemory(conversationId, memberHash, ConversationStep.CHAT,
                orderSn, null, null, null, summary, turns, Instant.now());
    }

    /** 空闲超时后：保留订单绑定，清空原文与确认态。 */
    public ConversationMemory resetAfterIdle() {
        return new ConversationMemory(conversationId, memberHash, ConversationStep.CHAT,
                orderSn, null, null, null, null, List.of(), Instant.now());
    }
}
