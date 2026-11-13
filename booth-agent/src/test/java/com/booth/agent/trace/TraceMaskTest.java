package com.booth.agent.trace;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraceMaskTest {

    @Test
    void hidesTokenAndPhone() {
        String summary = TraceMask.summarize(Map.of(
                "question", "联系 13812345678 处理",
                "token", "member-a-secret",
                "orderSn", "202610301234567890"));
        assertFalse(summary.contains("13812345678"));
        assertFalse(summary.contains("member-a-secret"));
        assertTrue(summary.contains("138****5678"));
        assertTrue(summary.contains("token=***"));
        assertTrue(summary.contains("202610301234567890"));
    }
}
