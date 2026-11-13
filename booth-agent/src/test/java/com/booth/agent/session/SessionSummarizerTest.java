package com.booth.agent.session;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionSummarizerTest {

    @Test
    void keepsAllWhenUnderWindow() {
        List<StoredTurn> turns = List.of(
                new StoredTurn("user", "你好"),
                new StoredTurn("assistant", "在的"));
        SessionSummarizer.Compact compact = SessionSummarizer.compact(null, turns, 4);
        assertEquals(2, compact.turns().size());
        assertEquals(null, compact.summary());
    }

    @Test
    void compressesOverflowIntoSummary() {
        List<StoredTurn> turns = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            turns.add(new StoredTurn("user", "问" + i));
            turns.add(new StoredTurn("assistant", "答" + i));
        }
        SessionSummarizer.Compact compact = SessionSummarizer.compact(null, turns, 12);
        assertEquals(12, compact.turns().size());
        assertTrue(compact.summary().contains("问1"));
        assertTrue(compact.summary().contains("答2"));
        assertEquals("问3", compact.turns().get(0).text());
    }
}
