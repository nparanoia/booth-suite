package com.booth.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.booth.agent.common.ToolErrorKind;
import com.booth.agent.common.ToolExecutionException;
import com.booth.agent.store.StoreApi;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Component
public class QueryOrderTool implements AgentTool {

    private final StoreApi storeApi;

    public QueryOrderTool(StoreApi storeApi) {
        this.storeApi = storeApi;
    }

    @Override
    public String name() {
        return ToolNames.QUERY_ORDER;
    }

    @Override
    public String description() {
        return "查询当前登录会员自己的订单状态和物流。订单不属于该会员时商城会拒绝，不要重试。";
    }

    @Override
    public Map<String, Object> jsonSchema() {
        return Schemas.object(Map.of("orderSn", Schemas.string("订单号")), List.of("orderSn"));
    }

    @Override
    public List<String> signatureKeys() {
        return List.of("orderSn");
    }

    @Override
    public String validate(Map<String, Object> args) {
        return Args.requireOrderSn(args);
    }

    @Override
    public ToolResult execute(ToolContext context, Map<String, Object> args) {
        String orderSn = Args.text(args, "orderSn");
        JsonNode data = storeApi.getOrder(context.memberToken(), orderSn);
        if (data == null || data.isNull() || !data.isObject()) {
            throw ToolExecutionException.booth(ToolErrorKind.REJECTED, 40401, "未查到订单");
        }
        String status = OrderJson.statusLabel(data.get("status"));
        String payAmount = OrderJson.text(data, "payAmount");
        String deliveryCompany = OrderJson.text(data, "deliveryCompany");
        String deliverySn = OrderJson.text(data, "deliverySn");
        StringBuilder reply = new StringBuilder();
        reply.append("订单 ").append(orderSn).append(" 状态：").append(status);
        if (payAmount != null) {
            reply.append("，实付 ").append(payAmount).append(" 元");
        }
        reply.append("。");
        if (StringUtils.hasText(deliveryCompany) || StringUtils.hasText(deliverySn)) {
            reply.append("物流：")
                    .append(deliveryCompany == null ? "" : deliveryCompany)
                    .append(" ")
                    .append(deliverySn == null ? "" : deliverySn)
                    .append("。");
        } else {
            reply.append("暂无物流单号（可能尚未发货）。");
        }
        return new ToolResult(reply.toString(), null, List.of());
    }
}
