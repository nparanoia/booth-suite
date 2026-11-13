package com.booth.agent.session;

import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 槽位抽取：订单号等结构化参数，避免各处复制正则。
 */
public final class SlotFiller {

    private static final Pattern ORDER_SN = Pattern.compile("(20\\d{16,}|\\d{18,})");
    /** 显式「手机/电话」优先，避免订单号里嵌套的 1xxxxxxxxxx 被当成手机。 */
    private static final Pattern PHONE_LABELED = Pattern.compile("(?:手机|电话|联系方式)[：:\\s]*(1\\d{10})");
    /** 独立 11 位号段：两侧不能再贴数字（挡住订单号内部假阳性）。 */
    private static final Pattern PHONE_STANDALONE = Pattern.compile("(?<!\\d)(1\\d{10})(?!\\d)");
    private static final Pattern NAME = Pattern.compile(
            "(?:收货人|收件人)[：:\\s]*([\\u4e00-\\u9fa5A-Za-z·]{2,16})");
    /** 「地址改成/改为/换成 …」优先于普通「地址…」，避免把「改成」吃进正文。 */
    private static final Pattern DETAIL_CHANGE = Pattern.compile("地址(?:改成|改为|换成)[：:\\s]*(.+)");
    /** 「地址上海市…」；`(?<!改)` 避免匹配「改地址」本身。 */
    private static final Pattern DETAIL_PLAIN = Pattern.compile("(?:详细地址|(?<!改)地址)[：:\\s]*(.+)");

    private SlotFiller() {
    }

    public static String extractOrderSn(String message) {
        if (!StringUtils.hasText(message)) {
            return null;
        }
        Matcher matcher = ORDER_SN.matcher(message.trim());
        return matcher.find() ? matcher.group(1) : null;
    }

    /** 整句几乎只是一个订单号（用户在补槽）。 */
    public static boolean isOrderSnReply(String message) {
        if (!StringUtils.hasText(message)) {
            return false;
        }
        String text = message.trim();
        String sn = extractOrderSn(text);
        if (sn == null) {
            return false;
        }
        // 去掉单号后只剩空白/标点才算「补槽回复」，「查订单 20…」不算
        String stripped = text.replace(sn, "").replaceAll("[\\s:：#\\-]", "");
        return stripped.isEmpty();
    }

    /** 从自然语言抽改地址字段；缺字段则对应 key 不出现。 */
    public static Map<String, Object> extractAddressArgs(String message) {
        Map<String, Object> args = new LinkedHashMap<>();
        if (!StringUtils.hasText(message)) {
            return args;
        }
        String text = message.trim();
        Matcher name = NAME.matcher(text);
        if (name.find()) {
            args.put("receiverName", name.group(1).trim());
        }
        Matcher labeled = PHONE_LABELED.matcher(text);
        if (labeled.find()) {
            args.put("receiverPhone", labeled.group(1));
        } else {
            Matcher phone = PHONE_STANDALONE.matcher(text);
            if (phone.find()) {
                args.put("receiverPhone", phone.group(1));
            }
        }
        String address = null;
        Matcher change = DETAIL_CHANGE.matcher(text);
        if (change.find()) {
            address = change.group(1).trim();
        } else {
            Matcher plain = DETAIL_PLAIN.matcher(text);
            if (plain.find()) {
                address = plain.group(1).trim();
            }
        }
        if (address != null) {
            address = stripTrailingSlots(address);
        }
        if (address != null && address.length() > 4) {
            args.put("receiverDetailAddress", address);
            args.put("receiverProvince", "未知省");
            args.put("receiverCity", "未知市");
            args.put("receiverRegion", "未知区");
        }
        return args;
    }

    /** 地址段后面若还粘着「收货人/手机」等，截掉以免整段塞进 detail。 */
    private static String stripTrailingSlots(String address) {
        String cut = address
                .replaceAll("(?=收货人|收件人|手机|电话).*$", "")
                .trim();
        return cut.isEmpty() ? address : cut;
    }
}
