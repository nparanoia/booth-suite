package com.booth.agent;

import com.booth.agent.chat.ChatRequest;
import com.booth.agent.chat.ChatResponse;
import com.booth.agent.config.BoothAgentProperties;
import com.booth.agent.support.Harness;
import com.booth.agent.support.Props;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 边界重叠块：转人工、改地址、写熔断、对话入库。 */
class BoundaryRemainingTest {

    private Harness harness;

    @BeforeEach
    void setUp() throws Exception {
        harness = new Harness(Props.rules());
    }

    @Test
    void humanTransferReturnsPhone() {
        ChatResponse response = harness.orchestrator.chat("Bearer member-a",
                new ChatRequest("转人工", "c-human", null, null));
        assertEquals("HUMAN", response.intent());
        assertTrue(response.reply().contains("400-800-8820"));
        assertTrue(response.reply().contains("接听"));
    }

    @Test
    void changeAddressWritesStoreDirectly() {
        harness.fakeStore.status = "WAIT_DELIVER";
        ChatResponse done = harness.orchestrator.chat("Bearer member-a",
                new ChatRequest("改地址 收货人张三 手机13800138000 地址上海市浦东新区示范路1号",
                        "c-addr", harness.fakeStore.orderSn, null));
        assertEquals("CHANGE_ADDRESS", done.intent());
        assertNull(done.confirm());
        assertTrue(done.reply().contains("收货地址已更新") || done.reply().contains("已更新"));
        assertEquals(1, harness.fakeStore.receiverUpdates.get());
        assertEquals("张三", harness.fakeStore.lastReceiver.get("receiverName"));
        assertEquals("13800138000", harness.fakeStore.lastReceiver.get("receiverPhone"));
    }

    @Test
    void changeAddressColloquialUtteranceWritesStore() {
        harness.fakeStore.status = "WAIT_DELIVER";
        String sn = harness.fakeStore.orderSn;
        ChatResponse done = harness.orchestrator.chat("Bearer member-a",
                new ChatRequest("把订单 " + sn + " 地址改成 北京市朝阳区测试路88号 收件人王五 手机13900001111",
                        "c-addr-colo", null, null));
        assertEquals("CHANGE_ADDRESS", done.intent());
        assertTrue(done.reply().contains("收货地址已更新") || done.reply().contains("已更新"),
                () -> "reply=" + done.reply());
        assertEquals(1, harness.fakeStore.receiverUpdates.get());
        assertEquals("王五", harness.fakeStore.lastReceiver.get("receiverName"));
    }

    @Test
    void changeAddressRejectsNonWaitDeliver() {
        harness.fakeStore.status = "COMPLETED";
        ChatResponse response = harness.orchestrator.chat("Bearer member-a",
                new ChatRequest("改地址 收货人张三 手机13800138000 地址上海市浦东新区示范路1号",
                        "c-addr2", harness.fakeStore.orderSn, null));
        assertTrue(response.reply().contains("待发货") || response.reply().contains("尚未修改"));
        assertEquals(0, harness.fakeStore.receiverUpdates.get());
    }

    @Test
    void writeCircuitBlocksDirectWrite() throws Exception {
        BoothAgentProperties.Agent.Governance g = harness.properties.agent().governance();
        BoothAgentProperties blocked = new BoothAgentProperties(
                harness.properties.gatewayBaseUrl(),
                new BoothAgentProperties.Agent(
                        harness.properties.agent().confirmTtl(),
                        harness.properties.agent().knowledgeMinScore(),
                        harness.properties.agent().vectorMinScore(),
                        harness.properties.agent().maxToolSteps(),
                        harness.properties.agent().timeoutRetries(),
                        harness.properties.agent().llm(),
                        harness.properties.agent().embedding(),
                        new BoothAgentProperties.Agent.Governance(
                                g.maxConcurrentPerUser(), g.maxRequestsPerMinute(), g.toolTimeout(),
                                g.requestDedupSeconds(), true),
                        harness.properties.agent().support(),
                        harness.properties.agent().skills()
                ));
        Harness circuit = new Harness(blocked, harness.store, harness.fakeStore);
        harness.fakeStore.status = "WAIT_DELIVER";
        ChatResponse blockedReply = circuit.orchestrator.chat("Bearer member-a",
                new ChatRequest("改地址 收货人李四 手机13900139000 地址北京市朝阳区示例路2号",
                        "c-circuit", harness.fakeStore.orderSn, null));
        assertTrue(blockedReply.reply().contains("熔断") || blockedReply.reply().contains("暂时关闭"));
        assertEquals(0, harness.fakeStore.receiverUpdates.get());
    }

    @Test
    void chatTranscriptPersisted() {
        harness.orchestrator.chat("Bearer member-a",
                new ChatRequest("转人工", "c-trace", null, null));
        List<Map<String, Object>> rows = harness.transcripts.search(null, "c-trace", null, 20);
        assertTrue(rows.size() >= 2);
        assertTrue(rows.stream().anyMatch(r -> "user".equals(r.get("role"))));
        assertTrue(rows.stream().anyMatch(r -> "assistant".equals(r.get("role"))));
    }

    @Test
    void afterSaleWizardAsksOrderFirst() {
        ChatResponse response = harness.orchestrator.chat("Bearer member-a",
                new ChatRequest("申请退货", "c-as", null, null));
        assertEquals("AFTER_SALE", response.intent());
        assertTrue(response.reply().contains("订单号"));
        assertTrue(response.reply().contains("直接提交") || response.reply().contains("提供"));
    }
}
