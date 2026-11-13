package com.booth.agent.chat;

import java.util.List;
import java.util.Map;

public record ChatResponse(
        String reply,
        String intent,
        String conversationId,
        /** 需要用户确认时返回，前端展示确认卡 */
        ConfirmCard confirm,
        List<String> sources,
        Map<String, Object> debug
) {
    public record ConfirmCard(
            String ticketId,
            String action,
            String orderSn,
            Long orderItemId,
            String reason,
            String summary
    ) {
    }
}
