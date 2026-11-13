package com.booth.agent;

import com.booth.agent.chat.ChatRequest;
import com.booth.agent.chat.ChatResponse;
import com.booth.agent.chat.ConfirmRequest;
import com.booth.agent.support.Harness;
import com.booth.agent.support.Props;
import com.booth.agent.trace.TurnTrace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 离线黄金集指标：工具选择 / 拒答 / 写操作确认。产出 target/golden-metrics.json 供简历引用。
 */
class GoldenMetricsReportTest {

    private static final String ORDER = "202610301234567890";
    private Harness harness;
    private final List<CaseResult> cases = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        harness = new Harness(Props.rules());
    }

    @Test
    void writeMetrics() throws Exception {
        check("policy_tool", () -> {
            ChatResponse r = chat("七天无理由怎么退", "m-policy");
            TurnTrace t = only("m-policy");
            return t.tools().equals(List.of("search_policy")) && r.sources().contains("return-policy.md");
        });
        check("refuse_outside", () -> {
            ChatResponse r = chat("退货政策里的海外仓怎么退", "m-miss");
            return r.reply().contains("不能瞎答") && only("m-miss").tools().equals(List.of("search_policy"));
        });
        check("query_tool", () -> {
            chat("查订单 " + ORDER, "m-own");
            return only("m-own").tools().equals(List.of("query_order"));
        });
        check("idor_no_retry", () -> {
            ChatResponse r = harness.orchestrator.chat("Bearer member-b",
                    new ChatRequest("查订单 " + ORDER, "m-other"));
            return r.reply().contains("订单不存在") && harness.fakeStore.gets.get() == 1
                    && only("member-b", "m-other").errorCode().equals("REJECTED");
        });
        check("prepare_writes_directly", () -> {
            ChatResponse r = chat("给订单 " + ORDER + " 申请退货", "m-prep");
            return r.confirm() == null && harness.fakeStore.applies.get() == 1
                    && only("m-prep").tools().equals(List.of("prepare_return"))
                    && (r.reply().contains("售后已提交") || r.reply().contains("9001"));
        });
        check("deprecated_confirm_no_write", () -> {
            ChatResponse r = harness.orchestrator.confirm(bearer(), new ConfirmRequest("any-ticket"));
            return r.reply().contains("直接办理") && harness.fakeStore.applies.get() == 0;
        });

        long passed = cases.stream().filter(CaseResult::ok).count();
        double rate = cases.isEmpty() ? 0 : (passed * 100.0 / cases.size());
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("total", cases.size());
        report.put("passed", passed);
        report.put("taskCompletionRate", Math.round(rate * 10) / 10.0);
        report.put("toolSelectionAccuracy", Math.round(rate * 10) / 10.0);
        report.put("cases", cases.stream().map(c -> Map.of("name", c.name(), "ok", c.ok())).toList());
        Path out = Path.of("target", "golden-metrics.json");
        Files.createDirectories(out.getParent());
        String json = harness.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(report);
        Files.writeString(out, json, StandardCharsets.UTF_8);
        assertTrue(passed == cases.size(), "golden metrics incomplete: " + json);
        assertTrue(rate >= 99.0);
    }

    private void check(String name, Case fn) {
        boolean ok;
        try {
            ok = fn.run();
        } catch (Exception exception) {
            ok = false;
        }
        cases.add(new CaseResult(name, ok));
        harness.fakeStore.gets.set(0);
        harness.fakeStore.applies.set(0);
        harness.fakeStore.cancels.set(0);
        harness.fakeStore.lists.set(0);
        harness.fakeStore.status = "COMPLETED";
    }

    private ChatResponse chat(String message, String conversationId) {
        return harness.orchestrator.chat(bearer(), new ChatRequest(message, conversationId));
    }

    private TurnTrace only(String conversationId) {
        return only("member-a", conversationId);
    }

    private TurnTrace only(String member, String conversationId) {
        List<TurnTrace> traces = harness.traces.find(member, conversationId);
        return traces.get(traces.size() - 1);
    }

    private static String bearer() {
        return "Bearer member-a";
    }

    @FunctionalInterface
    private interface Case {
        boolean run() throws Exception;
    }

    private record CaseResult(String name, boolean ok) {
    }
}
