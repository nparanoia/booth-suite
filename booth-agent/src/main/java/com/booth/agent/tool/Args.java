package com.booth.agent.tool;

import org.springframework.util.StringUtils;

import java.util.Map;

public final class Args {

    private Args() {
    }

    public static String text(Map<String, Object> args, String key) {
        Object value = args.get(key);
        if (value == null) {
            return "";
        }
        return value.toString().trim();
    }

    public static String requireOrderSn(Map<String, Object> args) {
        String orderSn = text(args, "orderSn");
        if (!StringUtils.hasText(orderSn)) {
            return "请提供订单号";
        }
        if (!orderSn.matches("^[0-9]{8,32}$")) {
            return "订单号格式不正确";
        }
        return null;
    }

    public static String checkReason(Map<String, Object> args) {
        if (!args.containsKey("reason") || args.get("reason") == null) {
            return null;
        }
        String reason = text(args, "reason");
        if (!StringUtils.hasText(reason)) {
            return null;
        }
        if (reason.length() > 64) {
            return "退货原因不能超过 64 个字";
        }
        return null;
    }

    /** 字段缺失返回 null；格式不对也返回 null，由调用方再区分。 */
    public static boolean present(Map<String, Object> args, String key) {
        if (!args.containsKey(key) || args.get(key) == null) {
            return false;
        }
        return !(args.get(key) instanceof String text) || StringUtils.hasText(text);
    }

    public static Long positiveLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String text && !StringUtils.hasText(text)) {
            return null;
        }
        try {
            long parsed;
            if (value instanceof Number number) {
                double asDouble = number.doubleValue();
                if (asDouble != Math.rint(asDouble)) {
                    return null;
                }
                parsed = number.longValue();
            } else {
                parsed = Long.parseLong(value.toString().trim());
            }
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
