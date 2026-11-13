package com.booth.agent.tool;

import com.booth.agent.common.ToolErrorKind;
import com.booth.agent.common.ToolExecutionException;

public final class ToolMessages {

    private ToolMessages() {
    }

    public static String userFacing(ToolExecutionException exception, int attempts) {
        if (exception.replyReady()) {
            return exception.getMessage();
        }
        return switch (exception.kind()) {
            case AUTH -> "没有权限或登录已失效。已停止，不会重试。";
            case TIMEOUT -> "商城响应超时，已重试 " + Math.max(0, attempts - 1) + " 次后停止。";
            case REJECTED -> "商城拒绝了这次操作：" + exception.getMessage() + "。已停止，不会重试。";
            default -> exception.getMessage();
        };
    }

    public static String sameArgs() {
        return "相同工具和参数已经失败，不再重试。";
    }

    public static String notAllowed() {
        return "只能使用查订单、查售后、准备退货、准备取消和查政策这些工具。";
    }

    public static String maxSteps(int maxSteps) {
        return "本轮工具调用已达到 " + maxSteps + " 次上限，已停止。";
    }

    public static String param(String reason, boolean terminal) {
        if (!terminal) {
            return reason;
        }
        return reason + " 参数错误已再试一次，已停止。";
    }

    public static String kindOf(ToolErrorKind kind) {
        return kind == null ? null : kind.name();
    }
}
