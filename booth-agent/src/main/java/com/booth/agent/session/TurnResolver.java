package com.booth.agent.session;

import com.booth.agent.intent.IntentRouter;
import com.booth.agent.intent.IntentType;

/**
 * 用上一轮槽位与确认态补全这一句。纯函数，不读存储。
 */
public final class TurnResolver {

    private TurnResolver() {
    }

    public static boolean returnFollowUp(String message) {
        String text = message == null ? "" : message.trim();
        return text.contains("就退这个")
                || text.contains("退这个")
                || text.contains("就退吧")
                || text.contains("退了吧")
                || text.contains("就按这个退");
    }

    public static IntentRouter.IntentDecision resolve(IntentRouter.IntentDecision raw, String message,
                                                      ConversationMemory memory) {
        String remembered = memory == null || memory.orderSn() == null || memory.orderSn().isBlank()
                ? null : memory.orderSn();

        // 待补订单号：用户只回了一个单号 → 默认回到售后（最常见补槽场景）
        if (memory != null
                && memory.step() == ConversationStep.NEED_ORDER_SN
                && SlotFiller.isOrderSnReply(message)
                && raw.orderSn() != null) {
            return new IntentRouter.IntentDecision(IntentType.AFTER_SALE, raw.orderSn());
        }

        if (returnFollowUp(message)) {
            String orderSn = raw.orderSn() != null ? raw.orderSn() : remembered;
            return new IntentRouter.IntentDecision(IntentType.AFTER_SALE, orderSn);
        }
        if (memory != null
                && memory.step() == ConversationStep.NEED_ORDER_SN
                && raw.orderSn() != null
                && raw.type() == IntentType.ORDER_QUERY
                && !looksLikeQuery(message)) {
            return new IntentRouter.IntentDecision(IntentType.AFTER_SALE, raw.orderSn());
        }
        if (raw.orderSn() == null
                && remembered != null
                && (raw.type() == IntentType.AFTER_SALE || raw.type() == IntentType.ORDER_QUERY
                || raw.type() == IntentType.CANCEL_ORDER || raw.type() == IntentType.CHANGE_ADDRESS)) {
            return new IntentRouter.IntentDecision(raw.type(), remembered);
        }
        return raw;
    }

    private static boolean looksLikeQuery(String message) {
        String text = message == null ? "" : message;
        return text.contains("查") || text.contains("物流") || text.contains("发货") || text.contains("到哪");
    }
}
