package com.booth.agent.write;

import java.time.Instant;

public record WriteActionRecord(
        long id,
        String ticketId,
        String memberHash,
        String action,
        String paramFingerprint,
        String orderSn,
        Long orderItemId,
        String conversationId,
        WriteActionStatus status,
        int attempts,
        int maxAttempts,
        String lastError,
        String resultRef,
        Instant createdAt,
        Instant updatedAt
) {
}
