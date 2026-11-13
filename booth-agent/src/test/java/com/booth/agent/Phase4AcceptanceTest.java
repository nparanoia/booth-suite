package com.booth.agent;

import com.booth.agent.chat.ChatListener;
import com.booth.agent.chat.ChatRequest;
import com.booth.agent.chat.ChatResponse;
import com.booth.agent.chat.ChatRun;
import com.booth.agent.chat.RunRegistry;
import com.booth.agent.common.ApiException;
import com.booth.agent.llm.ModelReply;
import com.booth.agent.llm.RequestedTool;
import com.booth.agent.support.Harness;
import com.booth.agent.support.Props;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase4AcceptanceTest {

    private static final String ORDER = "202610301234567890";

    @Test
    void showsProgressThenBody() throws Exception {
        Harness harness = new Harness(Props.rules());
        Recording listener = new Recording();
        ChatRun run = ChatRun.start("member-a", "conv-progress");

        ChatResponse order = harness.orchestrator.chat(bearer(),
                new ChatRequest("查订单 " + ORDER, "conv-progress"), run, listener);
        assertTrue(listener.progress.contains("正在查订单"));
        assertTrue(String.join("", listener.tokens).contains("已完成"));
        assertTrue(order.reply().contains("已完成"));
        assertEquals(1, harness.fakeStore.gets.get());
        assertEquals(0, harness.fakeStore.applies.get());

        Recording policy = new Recording();
        harness.orchestrator.chat(bearer(),
                new ChatRequest("七天无理由怎么退", "conv-policy"), ChatRun.start("member-a", "conv-policy"), policy);
        assertTrue(policy.progress.contains("正在检索"));
        String body = String.join("", policy.tokens);
        assertTrue(body.contains("7 天") || body.contains("未使用"));
        assertEquals(0, harness.fakeStore.applies.get());
    }

    @Test
    void cancelStopsLaterTokens() throws Exception {
        Harness harness = new Harness(Props.rules());
        Recording listener = new Recording();
        ChatRun run = ChatRun.start("member-a", "conv-cut");
        listener.onFirstToken = run::cancel;

        ChatResponse response = harness.orchestrator.chat(bearer(),
                new ChatRequest("查订单 " + ORDER, "conv-cut"), run, listener);

        assertEquals(1, listener.tokens.size());
        assertTrue(listener.cancelled);
        assertEquals("CANCELLED", response.debug().get("stoppedReason"));
        assertFalse(response.reply().contains("暂无物流"));
        assertEquals(1, harness.fakeStore.gets.get());
        assertEquals(0, harness.fakeStore.applies.get());
        assertNull(response.confirm());
    }

    @Test
    void cancelDoesNotSubmitReturn() throws Exception {
        Harness harness = new Harness(Props.rules());
        Recording listener = new Recording();
        ChatRun run = ChatRun.start("member-a", "conv-return");
        listener.onProgress = () -> {
            if (listener.progress.get(listener.progress.size() - 1).contains("正在提交退货")) {
                run.cancel();
            }
        };

        ChatResponse response = harness.orchestrator.chat(bearer(),
                new ChatRequest("给订单 " + ORDER + " 申请退货", "conv-return"), run, listener);

        assertTrue(listener.progress.contains("正在提交退货"));
        assertTrue(listener.cancelled);
        assertTrue(response.reply().contains("未提交"));
        assertNull(response.confirm());
        assertEquals(0, harness.fakeStore.gets.get());
        assertEquals(0, harness.fakeStore.applies.get());
    }

    @Test
    void finishedStreamSubmitsReturn() throws Exception {
        Harness harness = new Harness(Props.rules());
        Recording listener = new Recording();
        ChatResponse response = harness.orchestrator.chat(bearer(),
                new ChatRequest("给订单 " + ORDER + " 申请退货", "conv-card"),
                ChatRun.start("member-a", "conv-card"), listener);
        assertNull(response.confirm());
        assertTrue(String.join("", listener.tokens).contains("售后已提交")
                || response.reply().contains("售后已提交"));
        assertEquals(1, harness.fakeStore.applies.get());
    }

    @Test
    void cancelStopsTheNextModelCall() throws Exception {
        Harness harness = new Harness(Props.llm());
        harness.model.enqueue(new ModelReply(null, List.of(
                new RequestedTool("c1", "query_order", "{\"orderSn\":\"" + ORDER + "\"}"))));
        harness.model.enqueue(new ModelReply(null, List.of(
                new RequestedTool("c2", "prepare_return", "{\"orderSn\":\"" + ORDER + "\"}"))));
        Recording listener = new Recording();
        ChatRun run = ChatRun.start("member-a", "conv-model");
        listener.onProgress = () -> {
            if (listener.progress.get(listener.progress.size() - 1).contains("正在查订单")) {
                run.cancel();
            }
        };

        harness.orchestrator.chat(bearer(),
                new ChatRequest("查订单 " + ORDER, "conv-model"), run, listener);

        assertEquals(1, harness.model.calls);
        assertEquals(1, harness.fakeStore.gets.get());
        assertEquals(0, harness.fakeStore.applies.get());
        assertFalse(listener.progress.contains("正在提交退货"));
    }

    @Test
    void syncChatStillReturnsTheWholeReply() throws Exception {
        Harness harness = new Harness(Props.rules());
        ChatResponse response = harness.orchestrator.chat(bearer(),
                new ChatRequest("查订单 " + ORDER, "conv-sync"));
        assertTrue(response.reply().contains("暂无物流"));
        assertEquals(0, harness.fakeStore.applies.get());
    }

    @Test
    void cancelIsLimitedToTheSameMember() {
        RunRegistry registry = new RunRegistry();
        ChatRun run = registry.start("member-a", "conv-owner");
        ApiException denied = assertThrows(ApiException.class, () -> registry.cancel("member-b", run.id()));
        assertEquals(40301, denied.getCode());
        assertFalse(run.cancelled());
        assertTrue(registry.cancel("member-a", run.id()));
        assertFalse(registry.cancel("member-a", run.id()));
        assertFalse(registry.cancel("member-a", "missing"));
    }

    private static String bearer() {
        return "Bearer member-a";
    }

    private static final class Recording implements ChatListener {
        private final List<String> progress = new ArrayList<>();
        private final List<String> tokens = new ArrayList<>();
        private boolean cancelled;
        private Runnable onProgress = () -> {
        };
        private Runnable onFirstToken = () -> {
        };

        @Override
        public boolean enabled() {
            return true;
        }

        @Override
        public void progress(String text) {
            progress.add(text);
            onProgress.run();
        }

        @Override
        public void token(String text) {
            tokens.add(text);
            if (tokens.size() == 1) {
                onFirstToken.run();
            }
        }

        @Override
        public void cancelled() {
            cancelled = true;
        }
    }
}
