package com.booth.agent.session;

import java.time.Instant;

/** 确认动作的结果。票被删掉之后，靠它判断这次退货有没有真正提交过。 */
public record ActionLog(
        String ticketId,
        String member,
        String action,
        String orderSn,
        Long orderItemId,
        String conversationId,
        boolean success,
        int code,
        String returnId,
        String message,
        Instant at
) {
}
