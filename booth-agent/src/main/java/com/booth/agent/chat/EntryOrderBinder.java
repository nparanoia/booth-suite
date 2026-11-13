package com.booth.agent.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.booth.agent.common.ToolExecutionException;
import com.booth.agent.intent.IntentType;
import com.booth.agent.store.StoreApi;
import com.booth.agent.session.SessionService;
import com.booth.agent.session.SlotFiller;
import com.booth.agent.tool.OrderJson;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 入口上下文订单：校验归属后写入会话槽位；开场时自动查单并给短提示。
 */
@Component
public class EntryOrderBinder {

    public static final String BOOTSTRAP_USER_TEXT = "【入口关联订单】";

    private final StoreApi storeApi;
    private final SessionService sessionService;

    public EntryOrderBinder(StoreApi storeApi, SessionService sessionService) {
        this.storeApi = storeApi;
        this.sessionService = sessionService;
    }

    public ChatResponse bindAndOpen(String token, String conversationId, String rawOrderSn, String hint) {
        String orderSn = normalize(rawOrderSn);
        if (orderSn == null) {
            return deny(conversationId, "订单号格式不正确，未能关联。", null);
        }
        JsonNode data;
        try {
            data = storeApi.getOrder(token, orderSn);
        } catch (ToolExecutionException exception) {
            return deny(conversationId, "无法关联该订单：订单不存在或不属于当前账号。", orderSn);
        }
        if (data == null || !data.isObject()) {
            return deny(conversationId, "无法关联该订单：订单不存在或不属于当前账号。", orderSn);
        }
        sessionService.bindOrderSn(token, conversationId, orderSn);
        String status = OrderJson.statusLabel(data.get("status"));
        String tip = tipFor(hint, status);
        String reply = "已关联订单 " + orderSn + "，当前状态：" + status + "。" + tip;
        Map<String, Object> debug = new LinkedHashMap<>();
        debug.put("entryBound", true);
        debug.put("rememberedOrderSn", orderSn);
        debug.put("contextHint", hint == null ? "" : hint);
        debug.put("toolCalls", List.of(Map.of(
                "name", "query_order",
                "arguments", Map.of("orderSn", orderSn),
                "errorKind", "")));
        return new ChatResponse(reply, IntentType.ORDER_QUERY.name(), conversationId, null, List.of(), debug);
    }

    /** 仅绑定槽位（后续还有用户正文时用），失败返回错误回复。 */
    public ChatResponse bindOnly(String token, String conversationId, String rawOrderSn) {
        String orderSn = normalize(rawOrderSn);
        if (orderSn == null) {
            return deny(conversationId, "订单号格式不正确，未能关联。", null);
        }
        try {
            JsonNode data = storeApi.getOrder(token, orderSn);
            if (data == null || !data.isObject()) {
                return deny(conversationId, "无法关联该订单：订单不存在或不属于当前账号。", orderSn);
            }
        } catch (ToolExecutionException exception) {
            return deny(conversationId, "无法关联该订单：订单不存在或不属于当前账号。", orderSn);
        }
        sessionService.bindOrderSn(token, conversationId, orderSn);
        return null;
    }

    private static String normalize(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String extracted = SlotFiller.extractOrderSn(raw.trim());
        return extracted != null ? extracted : null;
    }

    private static String tipFor(String hint, String statusLabel) {
        String key = hint == null ? "" : hint.trim().toLowerCase();
        return switch (key) {
            case "address", "change_address" -> "可直接说改地址（待发货订单），或继续提问。";
            case "cancel" -> "可直接说取消订单（待付款），或继续提问。";
            case "return" -> "可直接说申请退货，或继续提问。";
            default -> "可直接说查物流、申请退货或改地址，或继续提问。";
        };
    }

    private static ChatResponse deny(String conversationId, String reply, String attempted) {
        Map<String, Object> debug = new LinkedHashMap<>();
        debug.put("entryBound", false);
        if (attempted != null) {
            debug.put("attemptedOrderSn", attempted);
        }
        return new ChatResponse(reply, IntentType.ORDER_QUERY.name(), conversationId, null, List.of(), debug);
    }
}
