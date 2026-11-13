package com.booth.agent.llm;

import com.booth.agent.tool.Args;
import com.booth.agent.tool.StepLog;
import com.booth.agent.tool.ToolContext;
import com.booth.agent.tool.ToolExecutor;
import com.booth.agent.tool.ToolNames;
import com.booth.agent.tool.TurnGuard;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Spring AI @Tool 声明。真正执行仍走 ToolExecutor + 白名单 AgentTool（含直接写商城）。
 */
@Component
public class MemberAssistantTools {

    public static final ThreadLocal<ToolContext> CONTEXT = new ThreadLocal<>();
    public static final ThreadLocal<TurnGuard> GUARD = new ThreadLocal<>();

    private final ToolExecutor executor;

    public MemberAssistantTools(ToolExecutor executor) {
        this.executor = executor;
    }

    @Tool(description = "查询当前登录会员自己的订单详情与状态")
    public String query_order(@ToolParam(description = "订单号") String orderSn) {
        return run(ToolNames.QUERY_ORDER, map("orderSn", orderSn));
    }

    @Tool(description = "列出当前会员的售后单")
    public String list_returns() {
        return run(ToolNames.LIST_RETURNS, Map.of());
    }

    @Tool(description = "检索退换货、运费险等店内政策，低于置信会拒答")
    public String search_policy(@ToolParam(description = "用户政策问题") String question) {
        return run(ToolNames.SEARCH_POLICY, map("question", question));
    }

    @Tool(description = "为已完成订单申请退货，校验通过后直接提交商城")
    public String prepare_return(
            @ToolParam(description = "订单号") String orderSn,
            @ToolParam(description = "订单明细 id，可空", required = false) Long orderItemId,
            @ToolParam(description = "退货原因", required = false) String reason) {
        Map<String, Object> args = map("orderSn", orderSn);
        if (orderItemId != null) {
            args.put("orderItemId", orderItemId);
        }
        if (reason != null && !reason.isBlank()) {
            args.put("reason", reason);
        }
        return run(ToolNames.PREPARE_RETURN, args);
    }

    @Tool(description = "取消待付款订单，校验通过后直接提交商城")
    public String prepare_cancel(@ToolParam(description = "订单号") String orderSn) {
        return run(ToolNames.PREPARE_CANCEL, map("orderSn", orderSn));
    }

    @Tool(description = "修改待发货订单收货地址，校验通过后直接提交商城")
    public String prepare_change_address(
            @ToolParam(description = "订单号") String orderSn,
            @ToolParam(description = "收货人") String receiverName,
            @ToolParam(description = "手机号") String receiverPhone,
            @ToolParam(description = "详细地址") String receiverDetailAddress,
            @ToolParam(description = "省", required = false) String receiverProvince,
            @ToolParam(description = "市", required = false) String receiverCity,
            @ToolParam(description = "区县", required = false) String receiverRegion) {
        Map<String, Object> args = map("orderSn", orderSn);
        args.put("receiverName", receiverName);
        args.put("receiverPhone", receiverPhone);
        args.put("receiverDetailAddress", receiverDetailAddress);
        if (receiverProvince != null) {
            args.put("receiverProvince", receiverProvince);
        }
        if (receiverCity != null) {
            args.put("receiverCity", receiverCity);
        }
        if (receiverRegion != null) {
            args.put("receiverRegion", receiverRegion);
        }
        return run(ToolNames.PREPARE_CHANGE_ADDRESS, args);
    }

    @Tool(description = "用户要求人工客服时，返回店主电话与服务时段")
    public String transfer_human() {
        return run(ToolNames.TRANSFER_HUMAN, Map.of());
    }

    private String run(String tool, Map<String, Object> args) {
        ToolContext context = CONTEXT.get();
        TurnGuard guard = GUARD.get();
        if (context == null || guard == null) {
            return "工具上下文未就绪";
        }
        if (!Args.text(args, "orderSn").isBlank() || tool.contains("order") || tool.contains("return")
                || tool.contains("cancel") || tool.contains("address")) {
            // orderSn 空串时去掉，避免校验失败
            if (args.containsKey("orderSn") && Args.text(args, "orderSn").isBlank()) {
                args = new LinkedHashMap<>(args);
                args.remove("orderSn");
            }
        }
        StepLog step = executor.run(guard, tool, args, context);
        return step.reply() == null ? "" : step.reply();
    }

    private static Map<String, Object> map(String key, String value) {
        Map<String, Object> args = new LinkedHashMap<>();
        if (value != null) {
            args.put(key, value);
        }
        return args;
    }
}
