package com.booth.agent;

import com.booth.agent.chat.ChatRequest;
import com.booth.agent.chat.ChatResponse;
import com.booth.agent.llm.ModelReply;
import com.booth.agent.llm.RequestedTool;
import com.booth.agent.support.Harness;
import com.booth.agent.support.Props;
import com.booth.agent.tool.AgentTool;
import com.booth.agent.tool.StepLog;
import com.booth.agent.tool.ToolContext;
import com.booth.agent.tool.ToolNames;
import com.booth.agent.tool.ToolRegistry;
import com.booth.agent.tool.ToolResult;
import com.booth.agent.tool.TurnGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolLoopTest {

    private static final String ORDER = "202610301234567890";

    private Harness harness;

    @BeforeEach
    void setUp() throws Exception {
        harness = new Harness(Props.llm());
    }

    @Test
    @DisplayName("读超时最多再试两次后停止")
    void timeoutRetriesTwiceThenStops() {
        harness.fakeStore.timeoutsLeft.set(5);
        StepLog step = query(ORDER);
        assertEquals("TIMEOUT", step.errorKind());
        assertEquals(3, step.attempts());
        assertTrue(step.retried());
        assertTrue(step.reply().contains("已重试 2 次"));
        assertEquals(3, harness.fakeStore.gets.get());
    }

    @Test
    @DisplayName("超时后的下一次成功会返回订单")
    void timeoutThenSucceeds() {
        harness.fakeStore.timeoutsLeft.set(2);
        StepLog step = query(ORDER);
        assertTrue(step.success());
        assertEquals(3, step.attempts());
        assertTrue(step.retried());
        assertTrue(step.reply().contains("已完成"));
        assertEquals(3, harness.fakeStore.gets.get());
    }

    @Test
    @DisplayName("401 立即停止")
    void authStopsImmediately() {
        harness.fakeStore.unauthorized = true;
        StepLog step = query(ORDER);
        assertEquals("AUTH", step.errorKind());
        assertEquals(1, step.attempts());
        assertFalse(step.retried());
        assertTrue(step.reply().contains("不会重试"));
        assertEquals(1, harness.fakeStore.gets.get());
    }

    @Test
    @DisplayName("相同参数连续失败不再打商城")
    void sameArgsStop() {
        harness.fakeStore.unauthorized = true;
        TurnGuard guard = harness.executor.newTurn();
        ToolContext context = context();
        Map<String, Object> args = Map.of("orderSn", ORDER);
        StepLog first = harness.executor.run(guard, ToolNames.QUERY_ORDER, args, context);
        StepLog second = harness.executor.run(guard, ToolNames.QUERY_ORDER, args, context);
        assertEquals("AUTH", first.errorKind());
        assertEquals("SAME_ARGS", second.errorKind());
        assertEquals(1, harness.fakeStore.gets.get());
    }

    @Test
    @DisplayName("参数错误可以换一组参数再试一次")
    void paramCanBeCorrectedOnce() {
        TurnGuard guard = harness.executor.newTurn();
        ToolContext context = context();
        StepLog bad = harness.executor.run(guard, ToolNames.QUERY_ORDER, Map.of("orderSn", "abc"), context);
        StepLog good = harness.executor.run(guard, ToolNames.QUERY_ORDER, Map.of("orderSn", ORDER), context);
        assertEquals("PARAM", bad.errorKind());
        assertFalse(bad.terminal());
        assertTrue(good.success());
        assertEquals(1, harness.fakeStore.gets.get());
    }

    @Test
    @DisplayName("同一组错误参数不会试第二次")
    void sameInvalidArgsStop() {
        TurnGuard guard = harness.executor.newTurn();
        ToolContext context = context();
        Map<String, Object> args = Map.of("orderSn", "abc");
        harness.executor.run(guard, ToolNames.QUERY_ORDER, args, context);
        StepLog second = harness.executor.run(guard, ToolNames.QUERY_ORDER, args, context);
        assertEquals("SAME_ARGS", second.errorKind());
        assertEquals(0, harness.fakeStore.gets.get());
    }

    @Test
    @DisplayName("换了参数仍然不合法就停止")
    void secondInvalidStops() {
        TurnGuard guard = harness.executor.newTurn();
        ToolContext context = context();
        harness.executor.run(guard, ToolNames.QUERY_ORDER, Map.of("orderSn", "abc"), context);
        StepLog second = harness.executor.run(guard, ToolNames.QUERY_ORDER, Map.of("orderSn", "12"), context);
        assertEquals("PARAM", second.errorKind());
        assertTrue(second.terminal());
        assertTrue(second.reply().contains("已停止"));
        assertEquals(0, harness.fakeStore.gets.get());
    }

    @Test
    @DisplayName("参数里的令牌不会留在轨迹里")
    void tokenIsStrippedFromArguments() {
        StepLog step = harness.executor.run(harness.executor.newTurn(), ToolNames.QUERY_ORDER,
                Map.of("orderSn", ORDER, "token", "secret-token", "authorization", "Bearer secret-token"),
                context());
        assertFalse(step.arguments().containsKey("token"));
        assertFalse(step.arguments().containsKey("authorization"));
        assertFalse(step.reply().contains("secret-token"));
    }

    @Test
    @DisplayName("一轮最多四次工具调用")
    void maxStepsFour() {
        for (int i = 0; i < 6; i++) {
            harness.model.enqueue(new ModelReply(null, List.of(new RequestedTool("c" + i, "list_returns", "{}"))));
        }
        ChatResponse response = harness.orchestrator.chat(bearer(), new ChatRequest("我的售后", "conv-max"));
        assertEquals(4, harness.fakeStore.lists.get());
        assertEquals(4, harness.model.calls);
        assertEquals("MAX_STEPS", response.debug().get("stoppedReason"));
        assertTrue(response.reply().contains("当前没有售后单"));
        assertEquals(0, harness.fakeStore.applies.get());
    }

    @Test
    @DisplayName("白名单之外的工具不会打商城")
    void toolNotOnAllowlist() {
        harness.model.enqueue(new ModelReply(null,
                List.of(new RequestedTool("x", "apply_return", "{\"orderSn\":\"" + ORDER + "\"}"))));
        ChatResponse response = harness.orchestrator.chat(bearer(), new ChatRequest("直接退掉", "conv-deny"));
        assertEquals("NOT_ALLOWED", response.debug().get("stoppedReason"));
        assertEquals(0, harness.fakeStore.applies.get());
        assertEquals(0, harness.fakeStore.gets.get());
        assertTrue(response.reply().contains("查政策"));
    }

    @Test
    @DisplayName("模型不能把待确认说成已经退款")
    void modelCannotOverrideToolFacts() {
        harness.model.enqueue(tool("query_order", "{\"orderSn\":\"" + ORDER + "\"}"));
        harness.model.enqueue(new ModelReply("钱已退回，售后已提交成功", List.of()));
        ChatResponse response = harness.orchestrator.chat(bearer(), new ChatRequest("查订单 " + ORDER, "conv-fact"));
        assertTrue(response.reply().contains("已完成"));
        assertFalse(response.reply().contains("钱已退回"));
        assertFalse(response.reply().contains("已提交成功"));
        assertEquals(1, harness.fakeStore.gets.get());
        assertEquals(0, harness.fakeStore.applies.get());
        assertTrue(harness.model.seen.get(0).get(0).content().contains("不可信"));
    }

    @Test
    @DisplayName("模型调用退货工具后直接写商城")
    void modelPrepareWritesDirectly() {
        harness.model.enqueue(tool("prepare_return", "{\"orderSn\":\"" + ORDER + "\"}"));
        harness.model.enqueue(new ModelReply("售后已提交", List.of()));
        ChatResponse response = harness.orchestrator.chat(bearer(), new ChatRequest("我要退货 " + ORDER, "conv-prep"));
        assertNull(response.confirm());
        assertTrue(response.reply().contains("售后已提交") || response.reply().contains("9001"));
        assertEquals(1, harness.fakeStore.applies.get());
    }

    @Test
    @DisplayName("业务问题模型不选工具时退回规则")
    void modelSkipFallsBackToRules() {
        harness.model.enqueue(new ModelReply("您的订单已发货", List.of()));
        ChatResponse response = harness.orchestrator.chat(bearer(), new ChatRequest("查订单 " + ORDER, "conv-fallback"));
        assertEquals(Boolean.TRUE, response.debug().get("llmFallback"));
        assertEquals("rule", response.debug().get("mode"));
        assertTrue(response.reply().contains("已完成"));
        assertFalse(response.reply().contains("已发货"));
        assertEquals(1, harness.fakeStore.gets.get());
    }

    @Test
    @DisplayName("闲聊不必调用工具")
    void chitchatUsesModelText() {
        harness.model.enqueue(new ModelReply("你好，需要查订单可以发我单号。", List.of()));
        ChatResponse response = harness.orchestrator.chat(bearer(), new ChatRequest("你好", "conv-hi"));
        assertEquals("你好，需要查订单可以发我单号。", response.reply());
        assertEquals(0, harness.fakeStore.gets.get());
        assertEquals("llm", response.debug().get("mode"));
    }

    @Test
    @DisplayName("参数错误的工具名也不能执行")
    void badJsonDoesNotCallStore() {
        harness.model.enqueue(new ModelReply(null, List.of(new RequestedTool("b", "query_order", "not-json"))));
        ChatResponse response = harness.orchestrator.chat(bearer(), new ChatRequest("查订单", "conv-json"));
        assertEquals("PARAM", firstError(response));
        assertEquals(0, harness.fakeStore.gets.get());
        assertTrue(response.reply().contains("不是合法 JSON"));
    }

    @Test
    @DisplayName("工具名不能重复注册")
    void duplicateToolRejected() {
        AgentTool tool = new NamedTool("query_order");
        assertThrows(IllegalStateException.class, () -> new ToolRegistry(List.of(tool, new NamedTool("query_order"))));
    }

    private StepLog query(String orderSn) {
        return harness.executor.run(harness.executor.newTurn(), ToolNames.QUERY_ORDER,
                Map.of("orderSn", orderSn), context());
    }

    private static ToolContext context() {
        return new ToolContext("member-a", "conv", "查订单");
    }

    private static String bearer() {
        return "Bearer member-a";
    }

    private static ModelReply tool(String name, String json) {
        return new ModelReply(null, List.of(new RequestedTool("call-1", name, json)));
    }

    @SuppressWarnings("unchecked")
    private static String firstError(ChatResponse response) {
        List<?> calls = (List<?>) response.debug().get("toolCalls");
        return String.valueOf(((Map<String, Object>) calls.get(0)).get("errorKind"));
    }

    private static final class NamedTool implements AgentTool {
        private final String name;

        private NamedTool(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String description() {
            return name;
        }

        @Override
        public Map<String, Object> jsonSchema() {
            return Map.of("type", "object");
        }

        @Override
        public List<String> signatureKeys() {
            return List.of();
        }

        @Override
        public String validate(Map<String, Object> args) {
            return null;
        }

        @Override
        public ToolResult execute(ToolContext context, Map<String, Object> args) {
            return new ToolResult("ok", null, List.of());
        }
    }
}
