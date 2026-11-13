package com.booth.agent.chat;

import com.booth.agent.config.BoothAgentProperties;
import com.booth.agent.intent.IntentRouter;
import com.booth.agent.intent.IntentType;
import com.booth.agent.session.SessionService;
import com.booth.agent.session.SlotFiller;
import com.booth.agent.session.TurnResolver;
import com.booth.agent.tool.RunSignal;
import com.booth.agent.tool.StepLog;
import com.booth.agent.tool.ToolContext;
import com.booth.agent.tool.ToolEvents;
import com.booth.agent.tool.ToolExecutor;
import com.booth.agent.tool.ToolNames;
import com.booth.agent.tool.TurnGuard;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 没有模型密钥时的降级：规则选一个白名单工具，仍走同一套步数和错误分类。 */
@Service
public class RuleChatService {

    private final IntentRouter intentRouter;
    private final ToolExecutor executor;
    private final SessionService sessionService;
    private final BoothAgentProperties properties;

    public RuleChatService(IntentRouter intentRouter, ToolExecutor executor, SessionService sessionService,
                           BoothAgentProperties properties) {
        this.intentRouter = intentRouter;
        this.executor = executor;
        this.sessionService = sessionService;
        this.properties = properties;
    }

    public ChatResponse chat(String token, ChatRequest request, String conversationId) {
        return chat(token, request, conversationId, RunSignal.NONE, ToolEvents.QUIET);
    }

    public ChatResponse chat(String token, ChatRequest request, String conversationId,
                             RunSignal run, ToolEvents events) {
        if (run != null && run.cancelled()) {
            return ChatViews.build("rule", IntentType.CHITCHAT.name(), conversationId, "已取消，未提交售后。",
                    null, List.of(), List.of(), "CANCELLED");
        }
        IntentRouter.IntentDecision decision = TurnResolver.resolve(
                intentRouter.route(request.message()),
                request.message(),
                sessionService.load(token, conversationId));
        if (decision.type() == IntentType.CHITCHAT) {
            return ChatViews.build("rule", IntentType.CHITCHAT.name(), conversationId, ChatViews.CHITCHAT,
                    null, List.of(), List.of(), null);
        }
        if (decision.type() == IntentType.HUMAN) {
            if (!properties.agent().skills().humanTransferEnabled()) {
                return ChatViews.build("rule", IntentType.HUMAN.name(), conversationId,
                        "当前未开通电话转人工，请继续在对话中说明问题。", null, List.of(), List.of(), null);
            }
            return runTool(token, conversationId, request.message(), run, events,
                    ToolNames.TRANSFER_HUMAN, Map.of(), decision.type());
        }
        if (decision.type() == IntentType.CHANGE_ADDRESS) {
            if (!properties.agent().skills().changeAddressEnabled()) {
                return ChatViews.build("rule", IntentType.CHANGE_ADDRESS.name(), conversationId,
                        "改地址技能暂未开启。如需人工协助请说「转人工」。", null, List.of(), List.of(), null);
            }
            if (decision.orderSn() == null) {
                return ChatViews.build("rule", IntentType.CHANGE_ADDRESS.name(), conversationId,
                        "【改地址·步骤1/3】请提供待发货订单号，或从订单详情点「找客服」自动带单。",
                        null, List.of(), List.of(), null);
            }
            Map<String, Object> address = SlotFiller.extractAddressArgs(request.message());
            if (!address.containsKey("receiverName") || !address.containsKey("receiverPhone")
                    || !address.containsKey("receiverDetailAddress")) {
                return ChatViews.build("rule", IntentType.CHANGE_ADDRESS.name(), conversationId,
                        "【改地址】订单 " + decision.orderSn()
                                + " 已关联。请按格式补充：收货人张三 手机13800138000 地址上海市浦东新区xx路1号（提交后直接改址）",
                        null, List.of(), List.of(), null);
            }
            Map<String, Object> args = new LinkedHashMap<>(address);
            args.put("orderSn", decision.orderSn());
            return runTool(token, conversationId, request.message(), run, events,
                    ToolNames.PREPARE_CHANGE_ADDRESS, args, decision.type());
        }
        if (decision.type() == IntentType.AFTER_SALE && decision.orderSn() == null) {
            return ChatViews.build("rule", IntentType.AFTER_SALE.name(), conversationId,
                    "【售后】请提供订单号，或从订单详情进入客服自动带单。提供后将直接提交售后申请。",
                    null, List.of(), List.of(), null);
        }
        String tool = switch (decision.type()) {
            case POLICY -> ToolNames.SEARCH_POLICY;
            case ORDER_QUERY -> ToolNames.QUERY_ORDER;
            case RETURN_LIST -> ToolNames.LIST_RETURNS;
            case AFTER_SALE -> ToolNames.PREPARE_RETURN;
            case CANCEL_ORDER -> ToolNames.PREPARE_CANCEL;
            case CHANGE_ADDRESS, HUMAN, CHITCHAT -> throw new IllegalStateException("已分支处理");
        };
        Map<String, Object> args = new LinkedHashMap<>();
        if (ToolNames.SEARCH_POLICY.equals(tool)) {
            args.put("question", request.message());
        }
        if (decision.orderSn() != null
                && (ToolNames.QUERY_ORDER.equals(tool)
                || ToolNames.PREPARE_RETURN.equals(tool)
                || ToolNames.PREPARE_CANCEL.equals(tool))) {
            args.put("orderSn", decision.orderSn());
        }
        return runTool(token, conversationId, request.message(), run, events, tool, args, decision.type());
    }

    private ChatResponse runTool(String token, String conversationId, String message, RunSignal run,
                                 ToolEvents events, String tool, Map<String, Object> args, IntentType intent) {
        ToolContext context = new ToolContext(token, conversationId, message, run, events);
        TurnGuard guard = executor.newTurn();
        StepLog step = executor.run(guard, tool, args, context);
        String reply = step.reply();
        String stopped = step.terminal() ? step.errorKind() : null;
        return ChatViews.build("rule", intent.name(), conversationId, reply,
                null, step.sources(), List.of(step), stopped);
    }
}
