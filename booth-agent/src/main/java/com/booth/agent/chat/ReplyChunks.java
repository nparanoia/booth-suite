package com.booth.agent.chat;

import java.util.ArrayList;
import java.util.List;

public final class ReplyChunks {

    public static final int SIZE = 12;

    private ReplyChunks() {
    }

    public static List<String> split(String text) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }
        List<String> parts = new ArrayList<>();
        int index = 0;
        while (index < text.length()) {
            int end = Math.min(text.length(), index + SIZE);
            parts.add(text.substring(index, end));
            index = end;
        }
        return parts;
    }
}
