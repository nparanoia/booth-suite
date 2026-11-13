package com.booth.agent.session;

import java.util.ArrayList;
import java.util.List;

/**
 * 超出窗口的旧轮次压成短摘要，不调模型。面试可讲：摘要 + 最近原文 = 上下文管理。
 */
final class SessionSummarizer {

    private static final int SUMMARY_LIMIT = 800;

    private SessionSummarizer() {
    }

    static Compact compact(String existingSummary, List<StoredTurn> turns, int maxMessages) {
        if (turns.size() <= maxMessages) {
            return new Compact(existingSummary, turns);
        }
        int drop = turns.size() - maxMessages;
        if (drop % 2 == 1) {
            drop = Math.min(turns.size(), drop + 1);
        }
        List<StoredTurn> overflow = turns.subList(0, drop);
        List<StoredTurn> kept = new ArrayList<>(turns.subList(drop, turns.size()));
        String merged = merge(existingSummary, overflow);
        return new Compact(merged, kept);
    }

    private static String merge(String existing, List<StoredTurn> overflow) {
        StringBuilder builder = new StringBuilder();
        if (existing != null && !existing.isBlank()) {
            builder.append(existing.trim());
        }
        for (StoredTurn turn : overflow) {
            if (builder.length() > 0) {
                builder.append('；');
            }
            String role = "assistant".equals(turn.role()) ? "客服" : "用户";
            builder.append(role).append('：').append(shorten(turn.text()));
        }
        String text = builder.toString();
        if (text.length() <= SUMMARY_LIMIT) {
            return text;
        }
        return text.substring(text.length() - SUMMARY_LIMIT);
    }

    private static String shorten(String text) {
        if (text == null) {
            return "";
        }
        String trimmed = text.replace('\n', ' ').trim();
        return trimmed.length() <= 80 ? trimmed : trimmed.substring(0, 80) + "…";
    }

    record Compact(String summary, List<StoredTurn> turns) {
    }
}
