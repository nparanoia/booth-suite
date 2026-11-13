package com.booth.agent.tool;

import com.booth.agent.config.BoothAgentProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/** 转人工：返回店主固定电话，不建座席工单。 */
@Component
public class TransferHumanTool implements AgentTool {

    private final BoothAgentProperties properties;

    public TransferHumanTool(BoothAgentProperties properties) {
        this.properties = properties;
    }

    @Override
    public String name() {
        return ToolNames.TRANSFER_HUMAN;
    }

    @Override
    public String description() {
        return "用户要求人工客服时，告知店主电话与可接听时段。不创建工单。";
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
        if (!properties.agent().skills().humanTransferEnabled()) {
            return new ToolResult("当前未开通电话转人工，请继续在对话中说明问题。", null, List.of());
        }
        var support = properties.agent().support();
        String reply = "店主电话：" + support.humanPhone()
                + "。\n接听说明：" + support.humanHours()
                + "。\n请直接拨打该号码；本店没有在线座席排队。投诉取证可在店主后台查看对话记录。";
        return new ToolResult(reply, null, List.of());
    }
}
