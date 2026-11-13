package com.booth.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.booth.agent.common.ToolErrorKind;
import com.booth.agent.common.ToolExecutionException;
import com.booth.agent.store.StoreApi;
import com.booth.agent.write.DirectWriteResult;
import com.booth.agent.write.WriteActionService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/** 待付款订单经 outbox/指纹直接取消。 */
@Component
public class PrepareCancelTool implements AgentTool {

    private final StoreApi storeApi;
    private final WriteActionService writeActions;

    public PrepareCancelTool(StoreApi storeApi, WriteActionService writeActions) {
        this.storeApi = storeApi;
        this.writeActions = writeActions;
    }

    @Override
    public String name() {
        return ToolNames.PREPARE_CANCEL;
    }

    @Override
    public String description() {
        return "取消当前会员的待付款订单，校验通过后直接提交到商城。";
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
    public boolean readOnly() {
        return false;
    }

    @Override
    public String validate(Map<String, Object> args) {
        return Args.requireOrderSn(args);
    }

    @Override
    public ToolResult execute(ToolContext context, Map<String, Object> args) {
        String orderSn = Args.text(args, "orderSn");
        JsonNode data = storeApi.getOrder(context.memberToken(), orderSn);
        if (data == null || !data.isObject()) {
            throw ToolExecutionException.booth(ToolErrorKind.REJECTED, 40401, "未查到订单");
        }
        String status = OrderJson.text(data, "status");
        if (status == null) {
            status = data.path("status").asText("");
        }
        if (!"WAIT_PAY".equals(status) && !"0".equals(status)) {
            throw ToolExecutionException.ready(ToolErrorKind.REJECTED, 30601,
                    "只有待付款订单可以取消，当前状态：" + OrderJson.statusLabel(data.get("status")) + "。");
        }
        DirectWriteResult written = writeActions.executeDirect(
                context.memberToken(),
                "CANCEL_ORDER",
                orderSn,
                null,
                null,
                null,
                context.conversationId()
        );
        return new ToolResult(written.reply(), null, List.of());
    }
}
