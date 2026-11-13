package com.booth.agent.intent;

import com.booth.agent.session.SlotFiller;
import org.springframework.stereotype.Component;

/**
 * 规则场景路由：售前政策 / 查单 / 售后 / 取消 / 改地址 / 转人工。
 */
@Component
public class IntentRouter {

    public IntentDecision route(String message) {
        String text = message == null ? "" : message.trim();
        String orderSn = SlotFiller.extractOrderSn(text);

        if (containsAny(text, "转人工", "人工客服", "找店主", "打电话", "联系店主", "真人客服", "人工服务")) {
            return new IntentDecision(IntentType.HUMAN, orderSn);
        }
        if (containsAny(text, "退货政策", "七天无理由", "换货", "退款时效", "怎么退", "如何退", "退换货")
                && !(containsAny(text, "申请", "帮我退", "我要退") && orderSn != null)) {
            return new IntentDecision(IntentType.POLICY, orderSn);
        }
        if (containsAny(text, "我的售后", "售后列表", "退货单", "售后单")) {
            return new IntentDecision(IntentType.RETURN_LIST, orderSn);
        }
        // 「地址改成…」须先于查单：否则带单号时会落到 ORDER_QUERY
        if (containsAny(text,
                "改地址", "修改地址", "更改地址", "换个地址", "换地址",
                "收货地址", "改收货", "改收件", "收件地址",
                "地址改成", "地址改为", "改成地址", "改到新地址", "更新地址")) {
            return new IntentDecision(IntentType.CHANGE_ADDRESS, orderSn);
        }
        if (containsAny(text, "取消订单", "取消这一单", "不要了取消", "帮我取消订单")
                || (orderSn != null && containsAny(text, "取消"))) {
            return new IntentDecision(IntentType.CANCEL_ORDER, orderSn);
        }
        if (containsAny(text, "申请退货", "申请售后", "退这一单", "帮我退货")
                || (orderSn != null && containsAny(text, "退货", "售后"))) {
            return new IntentDecision(IntentType.AFTER_SALE, orderSn);
        }
        if (orderSn != null || containsAny(text, "查订单", "订单详情", "物流", "发货了吗", "到哪了")) {
            return new IntentDecision(IntentType.ORDER_QUERY, orderSn);
        }
        return new IntentDecision(IntentType.CHITCHAT, orderSn);
    }

    private static boolean containsAny(String text, String... words) {
        for (String word : words) {
            if (text.contains(word)) {
                return true;
            }
        }
        return false;
    }

    public record IntentDecision(IntentType type, String orderSn) {
    }
}
