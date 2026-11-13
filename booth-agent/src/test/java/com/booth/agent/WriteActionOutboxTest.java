package com.booth.agent;

import com.booth.agent.chat.ChatRequest;
import com.booth.agent.chat.ChatResponse;
import com.booth.agent.confirm.ConfirmTicket;
import com.booth.agent.confirm.ParamFingerprints;
import com.booth.agent.support.Harness;
import com.booth.agent.support.Props;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 直写路径经 outbox + 指纹幂等。 */
class WriteActionOutboxTest {

    private Harness harness;

    @BeforeEach
    void setUp() throws Exception {
        harness = new Harness(Props.rules());
    }

    @Test
    void applyReturnWritesStoreDirectly() {
        ChatResponse a = harness.orchestrator.chat("Bearer member-a",
                new ChatRequest("给订单 202610301234567890 申请退货", "fp-a"));
        assertTrue(a.reply().contains("售后已提交") || a.reply().contains("9001"));
        assertEquals(1, harness.fakeStore.applies.get());
    }

    @Test
    void repeatedReturnIntentIsIdempotent() {
        ChatResponse first = harness.orchestrator.chat("Bearer member-a",
                new ChatRequest("给订单 202610301234567890 申请退货", "fp-idem-1"));
        assertTrue(first.reply().contains("售后已提交") || first.reply().contains("9001"));
        assertEquals(1, harness.fakeStore.applies.get());

        ChatResponse second = harness.orchestrator.chat("Bearer member-a",
                new ChatRequest("给订单 202610301234567890 申请退货", "fp-idem-2"));
        assertTrue(second.reply().contains("售后已提交") || second.reply().contains("未重复"));
        assertEquals(1, harness.fakeStore.applies.get(), "相同指纹不得再次调商城建单");
    }

    @Test
    void fingerprintStable() {
        String a = ParamFingerprints.of("APPLY_RETURN", "SN1", 1L, "七天无理由", "x");
        String b = ParamFingerprints.of("APPLY_RETURN", "sn1", 1L, "七天无理由", "x");
        assertEquals(a, b);
        ConfirmTicket ticket = harness.confirm.create("member-a", "APPLY_RETURN", "SN1", 1L,
                "七天无理由", "x", "c1");
        assertEquals(a, ticket.paramFingerprint());
    }
}
