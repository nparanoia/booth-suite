package com.booth.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;

public final class OrderJson {

    private OrderJson() {
    }

    public static String text(JsonNode node, String field) {
        if (node == null) {
            return null;
        }
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text == null || text.isBlank() ? null : text;
    }

    public static String statusLabel(JsonNode status) {
        if (status == null || status.isNull()) {
            return "未知";
        }
        if (status.isObject()) {
            if (status.hasNonNull("desc")) {
                return status.get("desc").asText();
            }
            return statusLabel(status.get("code"));
        }
        return switch (status.asText()) {
            case "WAIT_PAY", "0" -> "待付款";
            case "WAIT_DELIVER", "1" -> "待发货";
            case "DELIVERED", "2" -> "已发货";
            case "COMPLETED", "3" -> "已完成";
            case "CLOSED", "4" -> "已关闭";
            case "PENDING" -> "待处理";
            case "RETURNING" -> "退货中";
            case "REJECTED" -> "已拒绝";
            default -> status.asText();
        };
    }

    public static boolean completed(JsonNode status) {
        if (status == null || status.isNull()) {
            return false;
        }
        if (status.isObject()) {
            return completed(status.get("code")) || "已完成".equals(text(status, "desc"));
        }
        String raw = status.asText();
        return "COMPLETED".equals(raw) || "已完成".equals(raw) || "3".equals(raw);
    }

    public static boolean waitDeliver(JsonNode status) {
        if (status == null || status.isNull()) {
            return false;
        }
        if (status.isObject()) {
            return waitDeliver(status.get("code")) || "待发货".equals(text(status, "desc"));
        }
        String raw = status.asText();
        return "WAIT_DELIVER".equals(raw) || "待发货".equals(raw) || "1".equals(raw);
    }
}
