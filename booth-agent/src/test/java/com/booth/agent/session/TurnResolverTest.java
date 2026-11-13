package com.booth.agent.session;

import com.booth.agent.intent.IntentRouter;
import com.booth.agent.intent.IntentType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TurnResolverTest {

    private final IntentRouter router = new IntentRouter();

    @Test
    void followUpUsesRememberedOrder() {
        ConversationMemory memory = memory(ConversationStep.CHAT, "202610301234567890");
        IntentRouter.IntentDecision decision = TurnResolver.resolve(router.route("就退这个"), "就退这个", memory);
        assertEquals(IntentType.AFTER_SALE, decision.type());
        assertEquals("202610301234567890", decision.orderSn());
    }

    @Test
    void bareOrderNumberContinuesPendingReturn() {
        ConversationMemory memory = memory(ConversationStep.NEED_ORDER_SN, null);
        IntentRouter.IntentDecision decision = TurnResolver.resolve(
                router.route("202610301234567890"), "202610301234567890", memory);
        assertEquals(IntentType.AFTER_SALE, decision.type());
        assertEquals("202610301234567890", decision.orderSn());
    }

    @Test
    void bareOrderNumberStillQueriesWhenNotWaiting() {
        ConversationMemory memory = memory(ConversationStep.CHAT, null);
        IntentRouter.IntentDecision decision = TurnResolver.resolve(
                router.route("202610301234567890"), "202610301234567890", memory);
        assertEquals(IntentType.ORDER_QUERY, decision.type());
    }

    @Test
    void queryDoesNotBecomeReturn() {
        ConversationMemory memory = memory(ConversationStep.NEED_ORDER_SN, null);
        IntentRouter.IntentDecision decision = TurnResolver.resolve(
                router.route("查订单 202610301234567890"), "查订单 202610301234567890", memory);
        assertEquals(IntentType.ORDER_QUERY, decision.type());
        assertFalse(TurnResolver.returnFollowUp("七天无理由怎么退"));
        assertNull(TurnResolver.resolve(router.route("怎么退"), "怎么退", memory).orderSn());
        assertTrue(TurnResolver.resolve(router.route("怎么退"), "怎么退", memory).type() == IntentType.POLICY);
    }

    private static ConversationMemory memory(ConversationStep step, String orderSn) {
        return new ConversationMemory("conv", "hash", step, orderSn, null, null, null, null, null, null);
    }
}
