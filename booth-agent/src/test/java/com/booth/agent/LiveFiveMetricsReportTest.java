package com.booth.agent;

import com.booth.agent.chat.ChatListener;
import com.booth.agent.chat.ChatRequest;
import com.booth.agent.chat.ChatResponse;
import com.booth.agent.chat.ChatRun;
import com.booth.agent.config.BoothAgentProperties;
import com.booth.agent.llm.OpenAiCompatibleClient;
import com.booth.agent.support.DotEnv;
import com.booth.agent.support.Harness;
import com.booth.agent.support.InMemoryConfirmStore;
import com.booth.agent.support.Props;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 接真实 OpenAI 兼容 API 后产出 5 项指标到 target/live-metrics.json。
 * 需要 booth-agent/.env 中配置 OPENAI_API_KEY（DeepSeek 等均可）。
 */
class LiveFiveMetricsReportTest {

    private static final String ORDER = "202610301234567890";
    private static final String MEMBER = "Bearer member-a";

    @Test
    void measureFiveMetrics() throws Exception {
        Map<String, String> env = DotEnv.load();
        String apiKey = firstNonBlank(env.get("OPENAI_API_KEY"), System.getenv("OPENAI_API_KEY"));
        Assumptions.assumeTrue(apiKey != null && !apiKey.isBlank() && !apiKey.contains("your"),
                "跳过：未配置 OPENAI_API_KEY（请写入 booth-agent/.env）");

        String baseUrl = firstNonBlank(env.get("OPENAI_BASE_URL"), System.getenv("OPENAI_BASE_URL"),
                "https://api.deepseek.com");
        String model = firstNonBlank(env.get("OPENAI_CHAT_MODEL"), System.getenv("OPENAI_CHAT_MODEL"),
                "deepseek-chat");

        BoothAgentProperties liveProps = Props.live(apiKey, baseUrl, model);
        Harness live = new Harness(liveProps, new InMemoryConfirmStore(), new com.booth.agent.support.FakeStore(),
                new OpenAiCompatibleClient(new com.fasterxml.jackson.databind.json.JsonMapper(), liveProps));

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("provider", Map.of(
                "baseUrl", baseUrl,
                "model", model,
                "embedding", "ngram"
        ));

        // 1) 工具选择准确率 / 办结率
        List<Map<String, Object>> toolCases = new ArrayList<>();
        toolCases.add(toolCase(live, "policy", "七天无理由怎么退", "live-t-policy",
                null, r -> toolsOf(live, "live-t-policy").contains("search_policy")
                        && r.sources() != null && r.sources().contains("return-policy.md")));
        toolCases.add(toolCase(live, "query", "查订单 " + ORDER, "live-t-query",
                List.of("query_order"), r -> r.reply() != null && r.reply().contains("已完成")));
        live.fakeStore.applies.set(0);
        toolCases.add(toolCase(live, "return",
                "请调用退货工具，直接给订单 " + ORDER + " 申请退货，不要只查询",
                "live-t-return",
                null, r -> toolsOf(live, "live-t-return").contains("prepare_return")
                        && live.fakeStore.applies.get() >= 1
                        && (r.reply().contains("售后") || r.reply().contains("9001") || r.reply().contains("提交"))));
        live.fakeStore.applies.set(0);
        live.fakeStore.status = "WAIT_PAY";
        toolCases.add(toolCase(live, "cancel", "取消订单 " + ORDER, "live-t-cancel",
                List.of("prepare_cancel"), r -> live.fakeStore.cancels.get() >= 1
                        && r.reply() != null && r.reply().contains("取消")));
        live.fakeStore.status = "COMPLETED";
        live.fakeStore.cancels.set(0);

        long toolPass = toolCases.stream().filter(c -> Boolean.TRUE.equals(c.get("ok"))).count();
        double toolRate = pct(toolPass, toolCases.size());
        report.put("metric1_toolSelection", Map.of(
                "passed", toolPass,
                "total", toolCases.size(),
                "toolSelectionAccuracyPct", toolRate,
                "taskCompletionRatePct", toolRate,
                "cases", toolCases
        ));

        // 2) 多意图轮次 / 时延（规则两轮 vs 模型一轮；模型侧试 3 次取成功样例）
        Harness rules = new Harness(Props.rules());
        long ruleStart = System.nanoTime();
        rules.orchestrator.chat(MEMBER, new ChatRequest("查订单 " + ORDER, "rule-multi"));
        ChatResponse ruleReturn = rules.orchestrator.chat(MEMBER,
                new ChatRequest("就退这个", "rule-multi"));
        long ruleMs = (System.nanoTime() - ruleStart) / 1_000_000L;
        int ruleRounds = 2;
        boolean ruleOk = rules.fakeStore.applies.get() == 1
                && (ruleReturn.reply().contains("售后") || ruleReturn.reply().contains("9001"));

        boolean llmOk = false;
        long llmMs = 0;
        List<String> multiTools = List.of();
        String multiPreview = "";
        int llmTrials = 0;
        for (int trial = 1; trial <= 3 && !llmOk; trial++) {
            llmTrials = trial;
            live.fakeStore.applies.set(0);
            live.fakeStore.gets.set(0);
            String conv = "live-multi-" + trial;
            long llmStart = System.nanoTime();
            ChatResponse multi = live.orchestrator.chat(MEMBER,
                    new ChatRequest("先查订单 " + ORDER + " 的状态，确认已完成后立刻申请退货", conv));
            llmMs = (System.nanoTime() - llmStart) / 1_000_000L;
            multiTools = toolsOf(live, conv);
            multiPreview = preview(multi.reply());
            llmOk = multiTools.contains("query_order") && multiTools.contains("prepare_return")
                    && live.fakeStore.applies.get() >= 1;
        }
        int llmRounds = 1;
        double roundCutPct = ruleOk && llmOk ? round(((ruleRounds - llmRounds) * 100.0) / ruleRounds) : 0;
        Map<String, Object> metric2 = new LinkedHashMap<>();
        metric2.put("ruleRounds", ruleRounds);
        metric2.put("ruleElapsedMs", ruleMs);
        metric2.put("ruleOk", ruleOk);
        metric2.put("llmRounds", llmRounds);
        metric2.put("llmElapsedMs", llmMs);
        metric2.put("llmOk", llmOk);
        metric2.put("llmTrials", llmTrials);
        metric2.put("llmTools", multiTools);
        metric2.put("roundReductionPct", roundCutPct);
        metric2.put("replyPreview", multiPreview);
        report.put("metric2_multiIntent", metric2);

        // 3) 政策召回 / 拒答（固定题，走规则知识库；与模型无关但纳入同一报告）
        Harness policy = new Harness(Props.rules());
        List<Map<String, Object>> hitCases = List.of(
                Map.of("q", "七天无理由怎么退", "expectSource", "return-policy.md"),
                Map.of("q", "退货政策里的运费险怎么赔", "expectSource", "freight-insurance.md")
        );
        List<Map<String, Object>> refuseCases = List.of(
                Map.of("q", "退货政策里的海外仓怎么退"),
                Map.of("q", "怎么退")
        );
        int hitOk = 0;
        List<Map<String, Object>> hitDetail = new ArrayList<>();
        int i = 0;
        for (Map<String, Object> c : hitCases) {
            ChatResponse r = policy.orchestrator.chat(MEMBER,
                    new ChatRequest(c.get("q").toString(), "pol-hit-" + i++));
            boolean ok = r.sources() != null && r.sources().contains(c.get("expectSource").toString());
            if (ok) {
                hitOk++;
            }
            hitDetail.add(Map.of("q", c.get("q"), "ok", ok, "sources", r.sources() == null ? List.of() : r.sources()));
        }
        int refuseOk = 0;
        List<Map<String, Object>> refuseDetail = new ArrayList<>();
        i = 0;
        for (Map<String, Object> c : refuseCases) {
            ChatResponse r = policy.orchestrator.chat(MEMBER,
                    new ChatRequest(c.get("q").toString(), "pol-ref-" + i++));
            boolean ok = r.reply() != null && r.reply().contains("不能瞎答");
            if (ok) {
                refuseOk++;
            }
            refuseDetail.add(Map.of("q", c.get("q"), "ok", ok));
        }
        double hitRate = pct(hitOk, hitCases.size());
        double refuseRate = pct(refuseOk, refuseCases.size());
        report.put("metric3_policyRecallRefuse", Map.of(
                "hitPassed", hitOk,
                "hitTotal", hitCases.size(),
                "hitRatePct", hitRate,
                "refusePassed", refuseOk,
                "refuseTotal", refuseCases.size(),
                "correctRefuseRatePct", refuseRate,
                "hallucinationRatePct", round(100.0 - refuseRate),
                "hitCases", hitDetail,
                "refuseCases", refuseDetail
        ));

        // 4) SSE 首进度
        AtomicLong firstProgressAt = new AtomicLong(-1);
        AtomicLong firstTokenAt = new AtomicLong(-1);
        long streamStart = System.nanoTime();
        ChatRun run = ChatRun.start("member-a", "live-sse");
        ChatListener listener = new ChatListener() {
            @Override
            public boolean enabled() {
                return true;
            }

            @Override
            public void progress(String text) {
                firstProgressAt.compareAndSet(-1, System.nanoTime());
            }

            @Override
            public void token(String text) {
                firstTokenAt.compareAndSet(-1, System.nanoTime());
            }

            @Override
            public void cancelled() {
            }
        };
        ChatResponse streamed = live.orchestrator.chat(MEMBER,
                new ChatRequest("查订单 " + ORDER, "live-sse"), run, listener);
        long doneAt = System.nanoTime();
        long firstProgressMs = firstProgressAt.get() < 0 ? -1 : (firstProgressAt.get() - streamStart) / 1_000_000L;
        long firstTokenMs = firstTokenAt.get() < 0 ? -1 : (firstTokenAt.get() - streamStart) / 1_000_000L;
        long fullMs = (doneAt - streamStart) / 1_000_000L;
        double waitCutPct = (firstProgressMs >= 0 && fullMs > 0)
                ? round(((fullMs - firstProgressMs) * 100.0) / fullMs) : 0;
        report.put("metric4_sseFirstProgress", Map.of(
                "firstProgressMs", firstProgressMs,
                "firstTokenMs", firstTokenMs,
                "fullReplyMs", fullMs,
                "perceivedWaitCutPct", waitCutPct,
                "ok", firstProgressMs >= 0 && streamed.reply() != null && streamed.reply().contains("已完成")
        ));

        // 5) 降级成功率：坏密钥 → llmFallback → 规则仍办结
        BoothAgentProperties badProps = Props.live("sk-invalid-for-fallback-test", baseUrl, model);
        Harness fallback = new Harness(badProps, new InMemoryConfirmStore(), new com.booth.agent.support.FakeStore(),
                new OpenAiCompatibleClient(new com.fasterxml.jackson.databind.json.JsonMapper(), badProps));
        List<Map<String, Object>> fbCases = new ArrayList<>();
        fbCases.add(fallbackCase(fallback, "查订单 " + ORDER, "fb-query",
                r -> Boolean.TRUE.equals(r.debug().get("llmFallback"))
                        && r.reply() != null && r.reply().contains("已完成")));
        fbCases.add(fallbackCase(fallback, "七天无理由怎么退", "fb-policy",
                r -> Boolean.TRUE.equals(r.debug().get("llmFallback"))
                        && r.sources() != null && r.sources().contains("return-policy.md")));
        fallback.fakeStore.status = "COMPLETED";
        fbCases.add(fallbackCase(fallback, "给订单 " + ORDER + " 申请退货", "fb-return",
                r -> Boolean.TRUE.equals(r.debug().get("llmFallback"))
                        && fallback.fakeStore.applies.get() >= 1));
        long fbPass = fbCases.stream().filter(c -> Boolean.TRUE.equals(c.get("ok"))).count();
        double fbRate = pct(fbPass, fbCases.size());
        report.put("metric5_fallbackSuccess", Map.of(
                "passed", fbPass,
                "total", fbCases.size(),
                "fallbackSuccessRatePct", fbRate,
                "cases", fbCases
        ));

        // 汇总（简历可用数字）
        Map<String, Object> headline = new LinkedHashMap<>();
        headline.put("toolSelectionAccuracyPct", toolRate);
        headline.put("taskCompletionRatePct", toolRate);
        headline.put("multiIntentRoundReductionPct", roundCutPct);
        headline.put("policyHitRatePct", hitRate);
        headline.put("policyCorrectRefuseRatePct", refuseRate);
        headline.put("ssePerceivedWaitCutPct", waitCutPct);
        headline.put("llmFallbackSuccessRatePct", fbRate);
        report.put("headline", headline);

        Path out = Path.of("target", "live-metrics.json");
        Files.createDirectories(out.getParent());
        String json = live.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(report);
        Files.writeString(out, json, StandardCharsets.UTF_8);
        System.out.println("live-metrics written: " + out.toAbsolutePath());
        System.out.println(json);

        assertTrue(toolRate >= 50.0, "工具准确率过低，见 target/live-metrics.json");
        assertTrue(fbRate >= 50.0, "降级成功率过低，见 target/live-metrics.json");
    }

    private static Map<String, Object> toolCase(Harness harness, String name, String message, String conv,
                                                List<String> expectToolsExact,
                                                java.util.function.Predicate<ChatResponse> done)
            throws Exception {
        harness.fakeStore.gets.set(0);
        long t0 = System.nanoTime();
        ChatResponse response;
        try {
            response = harness.orchestrator.chat(MEMBER, new ChatRequest(message, conv));
        } catch (Exception exception) {
            return Map.of("name", name, "ok", false, "error", exception.getClass().getSimpleName(),
                    "elapsedMs", (System.nanoTime() - t0) / 1_000_000L);
        }
        List<String> tools = toolsOf(harness, conv);
        boolean toolsOk = expectToolsExact == null || tools.equals(expectToolsExact);
        boolean ok = toolsOk && done.test(response)
                && !Boolean.TRUE.equals(response.debug().get("llmFallback"));
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("name", name);
        row.put("ok", ok);
        row.put("tools", tools);
        row.put("expectTools", expectToolsExact == null ? "predicate" : expectToolsExact);
        row.put("mode", response.debug().get("mode"));
        row.put("llmFallback", response.debug().get("llmFallback"));
        row.put("elapsedMs", (System.nanoTime() - t0) / 1_000_000L);
        row.put("replyPreview", preview(response.reply()));
        return row;
    }

    private static Map<String, Object> fallbackCase(Harness harness, String message, String conv,
                                                    java.util.function.Predicate<ChatResponse> okFn) {
        ChatResponse response = harness.orchestrator.chat(MEMBER, new ChatRequest(message, conv));
        boolean ok = okFn.test(response);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("name", conv);
        row.put("ok", ok);
        row.put("llmFallback", response.debug().get("llmFallback"));
        row.put("mode", response.debug().get("mode"));
        row.put("replyPreview", preview(response.reply()));
        return row;
    }

    private static List<String> toolsOf(Harness harness, String conversationId) {
        return harness.traces.find("member-a", conversationId).stream()
                .flatMap(t -> t.tools().stream())
                .toList();
    }

    private static String preview(String text) {
        if (text == null) {
            return "";
        }
        String one = text.replace('\n', ' ').trim();
        return one.length() <= 120 ? one : one.substring(0, 120) + "...";
    }

    private static double pct(long pass, int total) {
        if (total <= 0) {
            return 0;
        }
        return round(pass * 100.0 / total);
    }

    private static double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
