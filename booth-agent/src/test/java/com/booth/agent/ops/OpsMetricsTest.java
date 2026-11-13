package com.booth.agent.ops;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.booth.agent.support.InMemoryConfirmStore;
import com.booth.agent.trace.TurnTrace;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpsMetricsTest {

    @Test
    void countsPolicyAndWrites() {
        OpsMetrics metrics = new OpsMetrics(new InMemoryConfirmStore(), new ObjectMapper());
        metrics.observe(new TurnTrace("c1", "h", "POLICY", true, List.of("search_policy"),
                "q", 1, null, Instant.now()));
        metrics.observe(new TurnTrace("c1", "h", "POLICY", false, List.of("search_policy"),
                "q", 1, null, Instant.now()));
        metrics.observe(new TurnTrace("c1", "h", "AFTER_SALE", false, List.of("apply_return"),
                "o", 1, null, Instant.now()));
        metrics.observe(new TurnTrace("c1", "h", "CANCEL_ORDER", false, List.of("cancel_order"),
                "o", 1, "400", Instant.now()));

        Map<String, Object> summary = metrics.summary();
        assertEquals(2, summary.get("chats"));
        assertEquals(1, summary.get("policyHits"));
        assertEquals(1, summary.get("policyMisses"));
        assertEquals(1, summary.get("writeReturns"));
        assertEquals(0, summary.get("writeCancels"));
        assertEquals(1, summary.get("writeFails"));
        assertEquals(2, ((Map<?, ?>) summary.get("tools")).get("search_policy"));
    }
}
