package com.booth.agent.intent;

import com.booth.agent.session.SlotFiller;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntentRouterChangeAddressTest {

    private final IntentRouter router = new IntentRouter();

    @Test
    void colloquialAddressChangeIsNotOrderQuery() {
        String msg = "把订单 202611132121361550450880514 地址改成 北京市朝阳区测试路88号 收件人王五 手机13900001111";
        IntentRouter.IntentDecision d = router.route(msg);
        assertEquals(IntentType.CHANGE_ADDRESS, d.type());
        assertEquals("202611132121361550450880514", d.orderSn());
    }

    @Test
    void classicChangeAddressStillWorks() {
        IntentRouter.IntentDecision d = router.route("改地址 收货人张三 手机13800138000 地址上海市浦东新区示范路1号");
        assertEquals(IntentType.CHANGE_ADDRESS, d.type());
    }

    @Test
    void extractReceiverAliasAndAddressChangePhrase() {
        Map<String, Object> args = SlotFiller.extractAddressArgs(
                "把订单 202611132121361550450880514 地址改成 北京市朝阳区测试路88号 收件人王五 手机13900001111");
        assertEquals("王五", args.get("receiverName"));
        assertEquals("13900001111", args.get("receiverPhone"));
        assertEquals("北京市朝阳区测试路88号", args.get("receiverDetailAddress"));
    }

    @Test
    void extractClassicFormat() {
        Map<String, Object> args = SlotFiller.extractAddressArgs(
                "改地址 收货人张三 手机13800138000 地址上海市浦东新区示范路1号");
        assertEquals("张三", args.get("receiverName"));
        assertEquals("13800138000", args.get("receiverPhone"));
        assertTrue(args.get("receiverDetailAddress").toString().contains("浦东"));
    }
}
