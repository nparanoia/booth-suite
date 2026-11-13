package com.booth.agent.chat;

import com.booth.agent.intent.IntentType;
import com.booth.agent.tool.StepLog;
import com.booth.agent.tool.ToolNames;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ChatViews {

    private ChatViews() {
    }

    public static final String CHITCHAT = "我可以帮你查订单、看售后，或回答退换货政策。试试：「七天无理由怎么退」或「查订单 订单号」。";

    public static String intentOf(String tool) {
        if (tool == null) {
            return IntentType.CHITCHAT.name();
        }
        return switch (tool) {
            case ToolNames.QUERY_ORDER -> IntentType.ORDER_QUERY.name();
            case ToolNames.LIST_RETURNS -> IntentType.RETURN_LIST.name();
            case ToolNames.PREPARE_RETURN -> IntentType.AFTER_SALE.name();
            case ToolNames.PREPARE_CANCEL -> IntentType.CANCEL_ORDER.name();
            case ToolNames.PREPARE_CHANGE_ADDRESS -> IntentType.CHANGE_ADDRESS.name();
            case ToolNames.TRANSFER_HUMAN -> IntentType.HUMAN.name();
            case ToolNames.SEARCH_POLICY -> IntentType.POLICY.name();
            default -> IntentType.CHITCHAT.name();
        };
    }

    public static ChatResponse finish(String mode, String conversationId, List<StepLog> steps,
                                      String modelOnlyText, String stoppedReason) {
        StepLog last = steps == null || steps.isEmpty() ? null : steps.get(steps.size() - 1);
        String reply;
        ChatResponse.ConfirmCard confirm = null;
        List<String> sources = List.of();
        String intent = IntentType.CHITCHAT.name();
        if (last == null) {
            reply = StringUtils.hasText(modelOnlyText) ? modelOnlyText.trim() : CHITCHAT;
        } else {
            // 多工具时正文优先用模型收束句；无收束句再用最后一步工具回复
            reply = StringUtils.hasText(modelOnlyText) ? modelOnlyText.trim() : last.reply();
            confirm = last.confirm();
            sources = mergeSources(steps);
            intent = intentOf(last.name());
            if (confirm != null && stoppedReason == null) {
                stoppedReason = "CONFIRM_REQUIRED";
            }
            // 政策命中时 intent 以检索为准，避免被后续无关工具盖掉
            for (int i = steps.size() - 1; i >= 0; i--) {
                if (ToolNames.SEARCH_POLICY.equals(steps.get(i).name()) && steps.get(i).success()) {
                    intent = IntentType.POLICY.name();
                    break;
                }
            }
        }
        return build(mode, intent, conversationId, reply, confirm, sources, steps == null ? List.of() : steps, stoppedReason);
    }

    private static List<String> mergeSources(List<StepLog> steps) {
        if (steps == null || steps.isEmpty()) {
            return List.of();
        }
        LinkedHashMap<String, Boolean> ordered = new LinkedHashMap<>();
        for (StepLog step : steps) {
            if (step.sources() == null) {
                continue;
            }
            for (String source : step.sources()) {
                if (StringUtils.hasText(source)) {
                    ordered.put(source, Boolean.TRUE);
                }
            }
        }
        return List.copyOf(ordered.keySet());
    }

    public static ChatResponse build(String mode, String intent, String conversationId, String reply,
                                     ChatResponse.ConfirmCard confirm, List<String> sources,
                                     List<StepLog> steps, String stoppedReason) {
        Map<String, Object> debug = new LinkedHashMap<>();
        debug.put("mode", mode);
        debug.put("stoppedReason", stoppedReason);
        debug.put("toolCallCount", steps.size());
        debug.put("toolCalls", steps.stream().map(StepLog::toMap).toList());
        return new ChatResponse(reply, intent, conversationId, confirm, sources, debug);
    }
}
