package com.booth.agent.confirm;

import java.time.Instant;

/**
 * 售后写操作确认卡。真正提交前只存参数，确认时带 ticketId 幂等执行。
 */
public record ConfirmTicket(
        String ticketId,
        String memberTokenHash,
        String action,
        String orderSn,
        Long orderItemId,
        String reason,
        String description,
        Instant expireAt,
        String conversationId,
        String paramFingerprint
) {
}
