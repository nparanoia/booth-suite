package com.booth.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.booth.agent.common.ToolErrorKind;
import com.booth.agent.common.ToolExecutionException;
import com.booth.agent.store.StoreApi;
import com.booth.agent.write.DirectWriteResult;
import com.booth.agent.write.WriteActionService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/** 校验通过后经 outbox/指纹直写商城申请退货。 */
@Component
public class PrepareReturnTool implements AgentTool {

    private final StoreApi storeApi;
    private final WriteActionService writeActions;

    public PrepareReturnTool(StoreApi storeApi, WriteActionService writeActions) {
        this.storeApi = storeApi;
        this.writeActions = writeActions;
    }

    @Override
    public String name() {
        return ToolNames.PREPARE_RETURN;
    }

    @Override
    public String description() {
        return "为当前会员的已完成订单申请退货，校验通过后直接提交到商城。";
    }

    @Override
    public Map<String, Object> jsonSchema() {
        Map<String, Object> properties = Map.of(
                "orderSn", Schemas.string("订单号"),
                "orderItemId", Schemas.integer("订单明细 id，不填则用第一件"),
                "reason", Schemas.string("退货原因，不超过 64 字")
        );
        return Schemas.object(properties, List.of("orderSn"));
    }

    @Override
    public List<String> signatureKeys() {
        return List.of("orderItemId", "orderSn", "reason");
    }

    @Override
    public boolean readOnly() {
        return false;
    }

    @Override
    public String validate(Map<String, Object> args) {
        String orderSnError = Args.requireOrderSn(args);
        if (orderSnError != null) {
            return orderSnError;
        }
        String reasonError = Args.checkReason(args);
        if (reasonError != null) {
            return reasonError;
        }
        if (Args.present(args, "orderItemId") && Args.positiveLong(args.get("orderItemId")) == null) {
            return "订单明细 id 必须是正整数";
        }
        return null;
    }

    @Override
    public ToolResult execute(ToolContext context, Map<String, Object> args) {
        String orderSn = Args.text(args, "orderSn");
        JsonNode data = storeApi.getOrder(context.memberToken(), orderSn);
        if (data == null || !data.isObject()) {
            throw ToolExecutionException.booth(ToolErrorKind.REJECTED, 40401, "未查到订单");
        }
        if (!OrderJson.completed(data.get("status"))) {
            throw ToolExecutionException.ready(ToolErrorKind.REJECTED, 30604,
                    "只有已完成的订单可以申请退货，当前状态：" + OrderJson.statusLabel(data.get("status")) + "。");
        }
        JsonNode items = data.path("items");
        if (!items.isArray() || items.isEmpty()) {
            throw ToolExecutionException.ready(ToolErrorKind.REJECTED, 40004, "订单没有可退明细。");
        }
        Long orderItemId = Args.present(args, "orderItemId") ? Args.positiveLong(args.get("orderItemId")) : null;
        JsonNode chosen = null;
        if (orderItemId == null) {
            chosen = firstWithId(items);
        } else {
            for (JsonNode item : items) {
                if (item.path("id").isNumber() && item.path("id").asLong() == orderItemId) {
                    chosen = item;
                    break;
                }
            }
        }
        if (chosen == null || !chosen.path("id").isNumber()) {
            throw ToolExecutionException.ready(ToolErrorKind.PARAM, 400, "订单里没有这件商品，请换一个明细编号。");
        }
        long itemId = chosen.path("id").asLong();
        String reason = Args.text(args, "reason");
        if (!StringUtils.hasText(reason)) {
            reason = context.message() != null && context.message().contains("质量") ? "质量问题" : "七天无理由";
        }
        String description = "由智能客服提交";
        DirectWriteResult written = writeActions.executeDirect(
                context.memberToken(),
                "APPLY_RETURN",
                orderSn,
                itemId,
                reason,
                description,
                context.conversationId()
        );
        return new ToolResult(written.reply(), null, List.of());
    }

    private static JsonNode firstWithId(JsonNode items) {
        for (JsonNode item : items) {
            if (item.path("id").isNumber()) {
                return item;
            }
        }
        return null;
    }
}
