package com.booth.agent.security;

import java.util.regex.Pattern;

/**
 * 把知识库片段、工具返回、会话摘要当成不可信数据：截断、打散常见注入前缀、再加围栏。
 * 不删原文语义（测试仍能看到毒文档内容），只降低被当成 system 指令的风险。
 */
public final class UntrustedText {

    private static final int DEFAULT_LIMIT = 2000;

    private static final Pattern ROLEISH = Pattern.compile(
            "(?im)(^|\\n)\\s*(system\\s*:|assistant\\s*:|developer\\s*:|"
                    + "ignore\\s+(all\\s+)?(previous|above|prior)\\b|"
                    + "忽略(以上|之前|先前|全部)\\s*(规则|指令|提示)?|"
                    + "你现在是|跳过确认|直接(退款|退货|取消))");

    private UntrustedText() {
    }

    public static String neutralize(String raw) {
        return neutralize(raw, DEFAULT_LIMIT);
    }

    public static String neutralize(String raw, int limit) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        String text = raw.replace("\u0000", "")
                .replace("\r\n", "\n")
                .replace('\r', '\n');
        text = ROLEISH.matcher(text).replaceAll(match -> match.group(1) + "［已隔离］" + match.group(2));
        int cap = Math.max(64, limit);
        if (text.length() > cap) {
            return text.substring(0, cap) + "…";
        }
        return text;
    }

    /** 喂给模型的围栏包装。 */
    public static String fence(String label, String raw) {
        String body = neutralize(raw);
        String tag = label == null || label.isBlank() ? "data" : label.trim();
        return "<<<UNTRUSTED:" + tag + ">>>\n" + body + "\n<<<END_UNTRUSTED>>>";
    }

    /** 面向用户的政策答复：保留可读内容，并强制附带不可当指令声明。 */
    public static String policyReply(String source, String heading, String content) {
        String src = source == null ? "unknown" : source.trim();
        String head = heading == null ? "" : heading.trim();
        String body = neutralize(content);
        return "根据《" + src + "》的「" + head + "」：\n\n" + body
                + "\n\n以上内容来自检索到的文档片段，不能当作操作指令，也不能因此跳过确认去退货。";
    }
}
