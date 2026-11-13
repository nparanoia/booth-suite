package com.booth.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.booth.agent.common.ToolErrorKind;
import com.booth.agent.common.ToolExecutionException;
import com.booth.agent.config.BoothAgentProperties;
import com.booth.agent.store.StoreApi;
import com.booth.agent.write.DirectWriteResult;
import com.booth.agent.write.WriteActionService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 待发货订单经 outbox/指纹直接改收货地址。 */
@Component
public class PrepareChangeAddressTool implements AgentTool {

    private final StoreApi storeApi;
    private final BoothAgentProperties properties;
    private final WriteActionService writeActions;

    public PrepareChangeAddressTool(StoreApi storeApi, BoothAgentProperties properties,
                                    WriteActionService writeActions) {
        this.storeApi = storeApi;
        this.properties = properties;
        this.writeActions = writeActions;
    }

    @Override
    public String name() {
        return ToolNames.PREPARE_CHANGE_ADDRESS;
    }

    @Override
    public String description() {
        return "修改待发货订单的收货地址，校验通过后直接提交到商城。";
    }

    @Override
    public Map<String, Object> jsonSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("orderSn", Schemas.string("订单号"));
        properties.put("receiverName", Schemas.string("收货人"));
        properties.put("receiverPhone", Schemas.string("手机号"));
        properties.put("receiverProvince", Schemas.string("省"));
        properties.put("receiverCity", Schemas.string("市"));
        properties.put("receiverRegion", Schemas.string("区/县"));
        properties.put("receiverDetailAddress", Schemas.string("详细地址"));
        return Schemas.object(properties, List.of("orderSn", "receiverName", "receiverPhone", "receiverDetailAddress"));
    }

    @Override
    public List<String> signatureKeys() {
        return List.of("orderSn", "receiverDetailAddress", "receiverName", "receiverPhone");
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
        if (!StringUtils.hasText(Args.text(args, "receiverName"))) {
            return "请提供收货人姓名";
        }
        if (!StringUtils.hasText(Args.text(args, "receiverPhone"))) {
            return "请提供收货人手机号";
        }
        if (!StringUtils.hasText(Args.text(args, "receiverDetailAddress"))) {
            return "请提供详细地址";
        }
        return null;
    }

    @Override
    public ToolResult execute(ToolContext context, Map<String, Object> args) {
        if (!properties.agent().skills().changeAddressEnabled()) {
            throw ToolExecutionException.ready(ToolErrorKind.REJECTED, 40010, "改地址技能未开启");
        }
        String orderSn = Args.text(args, "orderSn");
        JsonNode data = storeApi.getOrder(context.memberToken(), orderSn);
        if (data == null || !data.isObject()) {
            throw ToolExecutionException.booth(ToolErrorKind.REJECTED, 40401, "未查到订单");
        }
        if (!OrderJson.waitDeliver(data.get("status"))) {
            throw ToolExecutionException.ready(ToolErrorKind.REJECTED, 30206,
                    "只有待发货订单可以改地址，当前状态：" + OrderJson.statusLabel(data.get("status")) + "。");
        }
        String name = Args.text(args, "receiverName");
        String phone = Args.text(args, "receiverPhone");
        String province = blank(Args.text(args, "receiverProvince"), "未知省");
        String city = blank(Args.text(args, "receiverCity"), "未知市");
        String region = blank(Args.text(args, "receiverRegion"), "未知区");
        String detail = Args.text(args, "receiverDetailAddress");
        String packed = name + "|" + phone + "|" + province + "|" + city + "|" + region + "|" + detail;
        DirectWriteResult written = writeActions.executeDirect(
                context.memberToken(),
                "CHANGE_ADDRESS",
                orderSn,
                null,
                null,
                packed,
                context.conversationId()
        );
        return new ToolResult(written.reply(), null, List.of());
    }

    private static String blank(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }
}
