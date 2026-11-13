package com.booth.agent.intent;

public enum IntentType {
    /** 退换货政策等规则问答 */
    POLICY,
    /** 查询订单 / 物流字段 */
    ORDER_QUERY,
    /** 申请售后（需确认） */
    AFTER_SALE,
    /** 查我的售后单 */
    RETURN_LIST,
    /** 取消待付款订单（需确认） */
    CANCEL_ORDER,
    /** 待发货改收货地址（需确认） */
    CHANGE_ADDRESS,
    /** 转人工（给店主电话） */
    HUMAN,
    /** 闲聊或无法识别 */
    CHITCHAT
}
