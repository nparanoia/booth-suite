package com.booth.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.booth.agent.store.StoreApi;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ListReturnsTool implements AgentTool {

    private final StoreApi storeApi;

    public ListReturnsTool(StoreApi storeApi) {
        this.storeApi = storeApi;
    }

    @Override
    public String name() {
        return ToolNames.LIST_RETURNS;
    }

    @Override
    public String description() {
        return "查询当前登录会员的售后单列表，只读，不会创建或修改售后。";
    }

    @Override
    public Map<String, Object> jsonSchema() {
        return Schemas.object(Map.of(), List.of());
    }

    @Override
    public List<String> signatureKeys() {
        return List.of();
    }

    @Override
    public String validate(Map<String, Object> args) {
        return null;
    }

    @Override
    public ToolResult execute(ToolContext context, Map<String, Object> args) {
        JsonNode data = storeApi.listReturns(context.memberToken());
        if (data == null || data.isNull() || !data.isArray() || data.isEmpty()) {
            return new ToolResult("当前没有售后单。", null, List.of());
        }
        StringBuilder reply = new StringBuilder("你的售后单：\n");
        for (JsonNode item : data) {
            reply.append("- 单号 ").append(OrderJson.text(item, "id"))
                    .append(" / 订单 ").append(OrderJson.text(item, "orderSn"))
                    .append(" / 状态 ").append(OrderJson.statusLabel(item.get("status")))
                    .append('\n');
        }
        return new ToolResult(reply.toString().trim(), null, List.of());
    }
}
