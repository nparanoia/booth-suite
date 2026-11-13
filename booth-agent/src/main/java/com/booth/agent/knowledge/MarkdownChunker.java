package com.booth.agent.knowledge;

import java.util.ArrayList;
import java.util.List;

public final class MarkdownChunker {

    private MarkdownChunker() {
    }

    public static List<Chunk> split(String source, String markdown) {
        String normalized = markdown == null ? "" : markdown.replace("\r\n", "\n");
        List<Chunk> chunks = new ArrayList<>();
        String heading = "";
        StringBuilder body = new StringBuilder();
        boolean opened = false;
        for (String line : normalized.split("\n", -1)) {
            if (line.startsWith("#")) {
                if (opened) {
                    add(chunks, source, heading, body);
                }
                heading = line.replaceFirst("^#+\\s*", "").trim();
                body = new StringBuilder();
                opened = true;
            } else if (opened) {
                body.append(line).append('\n');
            }
        }
        if (opened) {
            add(chunks, source, heading, body);
        }
        return chunks;
    }

    private static void add(List<Chunk> chunks, String source, String heading, StringBuilder body) {
        String content = body.toString().trim();
        if (heading.isBlank() || content.isBlank()) {
            return;
        }
        chunks.add(new Chunk(source, heading, content));
    }

    public record Chunk(String source, String heading, String content) {
    }
}
