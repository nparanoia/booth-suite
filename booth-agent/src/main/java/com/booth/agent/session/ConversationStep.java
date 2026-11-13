package com.booth.agent.session;

public enum ConversationStep {
    /** 闲聊，或上一轮已经结束 */
    CHAT,
    /** 待补订单号 */
    NEED_ORDER_SN,
    /** 待确认退货 */
    AWAIT_CONFIRM
}
