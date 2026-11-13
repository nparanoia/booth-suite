package com.booth.agent;

import com.booth.agent.chat.ChatRequest;
import com.booth.agent.chat.ChatResponse;
import com.booth.agent.support.Harness;
import com.booth.agent.support.Props;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntryOrderBindTest {

    private static final String ORDER = "202610301234567890";

    private Harness harness;

    @BeforeEach
    void setUp() throws Exception {
        harness = new Harness(Props.rules());
    }

    @Test
    @DisplayName("入口带单：校验通过后开场并写入会话")
    void bindOpensAndRemembers() {
        harness.fakeStore.status = "WAIT_DELIVER";
        ChatResponse response = harness.orchestrator.chat("Bearer member-a",
                new ChatRequest(null, "entry-1", ORDER, "address"));
        assertTrue(response.reply().contains("已关联订单"));
        assertTrue(response.reply().contains("待发货"));
        assertTrue(response.reply().contains("改地址"));
        assertEquals(ORDER, response.debug().get("rememberedOrderSn"));
        assertEquals(Boolean.TRUE, response.debug().get("entryBound"));
        assertEquals(1, harness.fakeStore.gets.get());
        assertEquals(ORDER, harness.session.load("member-a", "entry-1").orderSn());
    }

    @Test
    @DisplayName("入口带单：别人的单不写入会话")
    void foreignOrderDenied() {
        ChatResponse response = harness.orchestrator.chat("Bearer member-b",
                new ChatRequest("", "entry-deny", ORDER, "query"));
        assertTrue(response.reply().contains("无法关联"));
        assertEquals(Boolean.FALSE, response.debug().get("entryBound"));
        var memory = harness.session.load("member-b", "entry-deny");
        assertNotNull(memory);
        assertEquals(null, memory.orderSn());
        assertEquals(1, harness.fakeStore.gets.get());
    }

    @Test
    @DisplayName("入口带单后下一句可沿用订单号直接退货")
    void followUpAfterBind() {
        harness.fakeStore.status = "COMPLETED";
        harness.orchestrator.chat("Bearer member-a",
                new ChatRequest(null, "entry-follow", ORDER, "return"));
        ChatResponse applied = harness.orchestrator.chat("Bearer member-a",
                new ChatRequest("申请退货", "entry-follow"));
        assertNull(applied.confirm());
        assertTrue(applied.reply().contains("售后已提交") || applied.reply().contains("9001"));
        assertEquals(1, harness.fakeStore.applies.get());
    }

    @Test
    @DisplayName("带单同时带正文：先绑定再直接办理")
    void bindThenUserMessage() {
        harness.fakeStore.status = "COMPLETED";
        ChatResponse response = harness.orchestrator.chat("Bearer member-a",
                new ChatRequest("申请退货", "entry-msg", ORDER, "return"));
        assertNull(response.confirm());
        assertTrue(response.reply().contains("售后已提交") || response.reply().contains("9001"));
        assertFalse(Boolean.TRUE.equals(response.debug().get("entryBound")));
        assertEquals(ORDER, harness.session.load("member-a", "entry-msg").orderSn());
        assertEquals(1, harness.fakeStore.applies.get());
    }
}
