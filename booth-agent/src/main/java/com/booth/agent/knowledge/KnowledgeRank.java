package com.booth.agent.knowledge;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 向量分和区别性短语一起决定是否命中。短语优先，避免只因为都出现「退货」就回答错政策。
 */
public final class KnowledgeRank {

    private KnowledgeRank() {
    }

    public static List<String> phrases(String question) {
        String compact = compact(question);
        List<String> longer = new ArrayList<>();
        List<String> fours = new ArrayList<>();
        for (int length = Math.min(8, compact.length()); length >= 4; length--) {
            for (int start = 0; start + length <= compact.length(); start++) {
                String phrase = compact.substring(start, start + length);
                if (length == 4) {
                    if (fours.size() < 24) {
                        fours.add(phrase);
                    }
                } else if (longer.size() < 16) {
                    longer.add(phrase);
                }
            }
        }
        longer.addAll(fours);
        return longer;
    }

    public static int phraseHits(String question, String heading, String content) {
        String haystack = compact((heading == null ? "" : heading) + (content == null ? "" : content));
        int hits = 0;
        for (String phrase : phrases(question)) {
            if (haystack.contains(phrase)) {
                hits++;
            }
        }
        return hits;
    }

    static String compact(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("[\\p{P}\\s]+", "");
    }

    public static Pick pick(List<Candidate> candidates, int phraseMin, double vectorMin) {
        if (candidates == null || candidates.isEmpty()) {
            return Pick.miss();
        }
        Candidate byPhrase = candidates.stream()
                .filter(candidate -> candidate.phraseHits() >= phraseMin)
                .max(Comparator.comparingInt(Candidate::phraseHits).thenComparingDouble(Candidate::cosine))
                .orElse(null);
        if (byPhrase != null) {
            return Pick.hit(byPhrase);
        }
        Candidate byVector = candidates.stream()
                .max(Comparator.comparingDouble(Candidate::cosine))
                .orElseThrow();
        if (byVector.cosine() >= vectorMin) {
            return Pick.hit(byVector);
        }
        return Pick.miss();
    }

    public record Candidate(String source, String heading, String content, double cosine, int phraseHits) {
    }

    public record Pick(boolean hit, Candidate candidate) {
        static Pick miss() {
            return new Pick(false, null);
        }

        static Pick hit(Candidate candidate) {
            return new Pick(true, candidate);
        }
    }
}
