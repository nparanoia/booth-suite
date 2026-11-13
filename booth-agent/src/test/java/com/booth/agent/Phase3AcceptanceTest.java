package com.booth.agent;

import com.booth.agent.chat.ChatRequest;
import com.booth.agent.chat.ChatResponse;
import com.booth.agent.confirm.ConfirmStore;
import com.booth.agent.session.SessionService;
import com.booth.agent.support.FakeStore;
import com.booth.agent.support.Harness;
import com.booth.agent.support.InMemoryConfirmStore;
import com.booth.agent.support.Props;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase3AcceptanceTest {

    private static final String ORDER = "202610301234567890";

    private ConfirmStore store;
    private FakeStore booth;
    private Harness harness;

    @BeforeEach
    void setUp() throws Exception {
        store = new InMemoryConfirmStore();
        booth = new FakeStore();
        harness = new Harness(Props.rules(), store, booth);
    }

    @Test
    @DisplayName("第二句就退这个沿用上一轮订单号并直接提交")
    void secondSentenceUsesPreviousOrder() {
        ChatResponse queried = harness.orchestrator.chat(bearer(),
                new ChatRequest("查订单 " + ORDER, "conv-follow"));
        assertTrue(queried.reply().contains("已完成"));
        assertEquals(ORDER, queried.debug().get("rememberedOrderSn"));

        ChatResponse applied = harness.orchestrator.chat(bearer(),
                new ChatRequest("就退这个", "conv-follow"));
        assertNull(applied.confirm());
        assertTrue(applied.reply().contains("售后已提交") || applied.reply().contains("9001"));
        assertEquals(1, booth.applies.get());
        assertEquals(2, booth.gets.get());
    }

    @Test
    @DisplayName("待补订单号时下一句只有单号会继续退货并直接提交")
    void orderNumberContinuesReturn() {
        ChatResponse asked = harness.orchestrator.chat(bearer(),
                new ChatRequest("申请退货", "conv-need"));
        assertTrue(asked.reply().contains("请提供订单号"));
        assertEquals("NEED_ORDER_SN", asked.debug().get("sessionStep"));
        assertEquals(0, booth.gets.get());
        assertNull(asked.confirm());

        ChatResponse applied = harness.orchestrator.chat(bearer(),
                new ChatRequest(ORDER, "conv-need"));
        assertNull(applied.confirm());
        assertTrue(applied.reply().contains("售后已提交") || applied.reply().contains("9001"));
        assertEquals(1, booth.applies.get());
        assertEquals(1, booth.gets.get());
    }

    @Test
    @DisplayName("别人的会话接不到这张订单")
    void otherMemberDoesNotInheritOrder() {
        harness.orchestrator.chat(bearer(), new ChatRequest("查订单 " + ORDER, "conv-shared"));
        ChatResponse other = harness.orchestrator.chat("Bearer member-b",
                new ChatRequest("就退这个", "conv-shared"));
        assertNull(other.confirm());
        assertTrue(other.reply().contains("请提供订单号") || other.reply().contains("订单号"));
        assertEquals(0, booth.applies.get());
        assertEquals(1, booth.gets.get());
    }

    @Test
    @DisplayName("只保留最近十轮原文，更早的压进摘要")
    void windowKeepsTenRounds() {
        for (int i = 1; i <= 11; i++) {
            harness.orchestrator.chat(bearer(), new ChatRequest("轮次" + i, "conv-window"));
        }
        var memory = harness.session.load("member-a", "conv-window");
        assertEquals(SessionService.WINDOW, memory.turns().size() / 2);
        assertEquals("轮次2", memory.turns().get(0).text());
        assertEquals("user", memory.turns().get(0).role());
        assertEquals("轮次11", memory.turns().get(memory.turns().size() - 2).text());
        assertNotNull(memory.summary());
        assertTrue(memory.summary().contains("轮次1"));
        assertTrue(Boolean.TRUE.equals(
                harness.orchestrator.chat(bearer(), new ChatRequest("再问一句", "conv-window"))
                        .debug().get("sessionSummary")));
    }

    @Test
    @DisplayName("重启后仍能用会话里的订单号直接退货")
    void restartedSessionStillRemembersOrder() throws Exception {
        harness.orchestrator.chat(bearer(), new ChatRequest("查订单 " + ORDER, "conv-live"));
        Harness restarted = new Harness(Props.rules(), store, new FakeStore());
        ChatResponse applied = restarted.orchestrator.chat(bearer(),
                new ChatRequest("就退这个", "conv-live"));
        assertNull(applied.confirm());
        assertTrue(applied.reply().contains("售后已提交") || applied.reply().contains("9001"));
        assertEquals(1, restarted.fakeStore.applies.get());
        assertEquals(1, restarted.fakeStore.gets.get());
    }

    private static String bearer() {
        return "Bearer member-a";
    }
}
