package com.booth.agent.tool;

public final class ToolProgress {

    private ToolProgress() {
    }

    public static String text(String tool) {
        if (tool == null) {
            return "正在处理";
        }
        return switch (tool) {
            case ToolNames.QUERY_ORDER -> "正在查订单";
            case ToolNames.LIST_RETURNS -> "正在查售后";
            case ToolNames.PREPARE_RETURN -> "正在提交退货";
            case ToolNames.PREPARE_CANCEL -> "正在取消订单";
            case ToolNames.PREPARE_CHANGE_ADDRESS -> "正在改地址";
            case ToolNames.SEARCH_POLICY -> "正在检索";
            default -> "正在处理";
        };
    }
}
