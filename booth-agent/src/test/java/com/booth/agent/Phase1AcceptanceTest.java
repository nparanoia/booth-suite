package com.booth.agent;

import com.booth.agent.chat.ChatRequest;
import com.booth.agent.chat.ChatResponse;
import com.booth.agent.chat.ConfirmRequest;
import com.booth.agent.common.ApiException;
import com.booth.agent.support.Harness;
import com.booth.agent.support.Props;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase1AcceptanceTest {

    private static final String ORDER = "202610301234567890";

    private Harness harness;

    @BeforeEach
    void setUp() throws Exception {
        harness = new Harness(Props.rules());
        harness.model.forbid = true;
    }

    @Test
    @DisplayName("查自己的订单成功")
    void queryOwnOrder() {
        ChatResponse response = harness.orchestrator.chat(bearer("member-a"),
                new ChatRequest("查订单 " + ORDER, null));

        assertTrue(response.reply().contains("已完成"));
        assertTrue(response.reply().contains("12.34"));
        assertTrue(response.reply().contains("暂无物流"));
        assertEquals("rule", response.debug().get("mode"));
        assertEquals(1, harness.fakeStore.gets.get());
        Map<?, ?> call = firstCall(response);
        assertEquals("query_order", call.get("name"));
        assertEquals(1, ((Number) call.get("attempts")).intValue());
        assertEquals(Boolean.TRUE, call.get("success"));
        assertNull(response.confirm());
    }

    @Test
    @DisplayName("查别人的订单被商城拒绝且不再重试")
    void queryOtherOrderDoesNotRetry() {
        ChatResponse response = harness.orchestrator.chat(bearer("member-b"),
                new ChatRequest("查订单 " + ORDER, "conv-other"));

        assertTrue(response.reply().contains("订单不存在"));
        assertTrue(response.reply().contains("不会重试"));
        assertFalse(response.reply().toLowerCase().contains("exception"));
        assertEquals("REJECTED", response.debug().get("stoppedReason"));
        assertEquals(1, ((Number) response.debug().get("toolCallCount")).intValue());
        Map<?, ?> call = firstCall(response);
        assertEquals(1, ((Number) call.get("attempts")).intValue());
        assertEquals(Boolean.FALSE, call.get("retried"));
        assertEquals("REJECTED", call.get("errorKind"));
        assertEquals(1, harness.fakeStore.gets.get());
        assertEquals(0, harness.fakeStore.applies.get());
    }

    @Test
    @DisplayName("申请退货校验通过后直接写商城")
    void applyReturnWritesDirectly() {
        ChatResponse response = harness.orchestrator.chat(bearer("member-a"),
                new ChatRequest("给订单 " + ORDER + " 申请退货", "conv-return"));

        assertNull(response.confirm());
        assertTrue(response.reply().contains("售后已提交") || response.reply().contains("9001"));
        assertEquals(1, harness.fakeStore.applies.get());
        assertEquals(1001L, ((Number) harness.fakeStore.lastApply.get("orderItemId")).longValue());
        assertEquals("七天无理由", harness.fakeStore.lastApply.get("reason"));
        assertEquals(1, harness.fakeStore.gets.get());
    }

    @Test
    @DisplayName("废弃确认接口提示改走对话")
    void confirmEndpointDeprecated() {
        ChatResponse response = harness.orchestrator.confirm(bearer("member-a"),
                new ConfirmRequest("any-ticket"));
        assertTrue(response.reply().contains("直接办理") || response.debug().get("deprecatedConfirm") != null);
        assertEquals(0, harness.fakeStore.applies.get());
    }

    @Test
    @DisplayName("政策问答不写商城")
    void policyDoesNotWrite() {
        ChatResponse response = harness.orchestrator.chat(bearer("member-a"),
                new ChatRequest("七天无理由怎么退", "conv-policy"));
        assertTrue(response.reply().contains("七天") || response.reply().contains("7 天"));
        assertTrue(response.sources().contains("return-policy.md"));
        assertEquals(0, harness.fakeStore.gets.get());
        assertEquals(0, harness.fakeStore.applies.get());
        assertEquals("search_policy", firstCall(response).get("name"));
    }

    @Test
    @DisplayName("知识库没有命中时拒答")
    void policyMissRefuses() {
        ChatResponse response = harness.orchestrator.chat(bearer("member-a"),
                new ChatRequest("怎么退", "conv-miss"));
        assertTrue(response.reply().contains("不能瞎答"));
        assertTrue(response.sources().isEmpty());
        assertEquals(0, harness.fakeStore.applies.get());
    }

    @Test
    @DisplayName("没带订单号不会打商城")
    void missingOrderSnDoesNotCallStore() {
        ChatResponse response = harness.orchestrator.chat(bearer("member-a"),
                new ChatRequest("查订单", "conv-missing"));
        assertTrue(response.reply().contains("请提供订单号"));
        assertEquals("PARAM", firstCall(response).get("errorKind"));
        assertEquals(0, ((Number) firstCall(response).get("attempts")).intValue());
        assertEquals(0, harness.fakeStore.gets.get());
    }

    @Test
    @DisplayName("未完成订单不写售后")
    void unfinishedOrderDoesNotWrite() {
        harness.fakeStore.status = "WAIT_PAY";
        ChatResponse response = harness.orchestrator.chat(bearer("member-a"),
                new ChatRequest("给订单 " + ORDER + " 申请退货", "conv-wait"));
        assertNull(response.confirm());
        assertTrue(response.reply().contains("已完成") || response.reply().contains("不能"));
        assertEquals(0, harness.fakeStore.applies.get());
    }

    @Test
    @DisplayName("取消待付款直接写商城")
    void cancelWritesDirectly() {
        harness.fakeStore.status = "WAIT_PAY";
        ChatResponse cancelled = harness.orchestrator.chat(bearer("member-a"),
                new ChatRequest("取消订单 " + ORDER, "conv-cancel"));
        assertNull(cancelled.confirm());
        assertTrue(cancelled.reply().contains("已取消"));
        assertEquals(1, harness.fakeStore.cancels.get());
    }

    @Test
    @DisplayName("未登录直接拒绝")
    void missingToken() {
        ApiException exception = assertThrows(ApiException.class,
                () -> harness.orchestrator.chat(null, new ChatRequest("你好", null)));
        assertEquals(40100, exception.getCode());
        assertEquals(0, harness.fakeStore.gets.get());
    }

    @SuppressWarnings("unchecked")
    private static Map<?, ?> firstCall(ChatResponse response) {
        List<?> calls = (List<?>) response.debug().get("toolCalls");
        assertEquals(1, calls.size());
        return (Map<?, ?>) calls.get(0);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
