package com.booth.agent.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.booth.agent.common.ToolErrorKind;
import com.booth.agent.common.ToolExecutionException;
import com.booth.agent.store.StoreApi;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/** 按 booth-cloud 的业务码说话：别人的订单是 HTTP 200 语义下的 30201，不是再试一次就能成功。 */
public final class FakeStore implements StoreApi {

    public final String ownerToken = "member-a";
    public final String orderSn = "202610301234567890";
    public String status = "COMPLETED";
    public boolean unauthorized;
    public final AtomicInteger gets = new AtomicInteger();
    public final AtomicInteger lists = new AtomicInteger();
    public final AtomicInteger applies = new AtomicInteger();
    public final AtomicInteger cancels = new AtomicInteger();
    public final AtomicInteger receiverUpdates = new AtomicInteger();
    public final AtomicInteger timeoutsLeft = new AtomicInteger();
    public Map<String, Object> lastApply;
    public Map<String, Object> lastReceiver;

    @Override
    public JsonNode getOrder(String token, String orderSn) {
        gets.incrementAndGet();
        int pending = timeoutsLeft.getAndUpdate(value -> value > 0 ? value - 1 : 0);
        if (pending > 0) {
            throw ToolExecutionException.booth(ToolErrorKind.TIMEOUT, 504, "商城响应超时");
        }
        if (unauthorized) {
            throw ToolExecutionException.booth(ToolErrorKind.AUTH, 401, "未登录或登录已过期");
        }
        if (!ownerToken.equals(token) || !this.orderSn.equals(orderSn)) {
            throw ToolExecutionException.booth(ToolErrorKind.REJECTED, 30201, "订单不存在");
        }
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.put("orderSn", this.orderSn);
        node.put("status", status);
        node.put("payAmount", "12.34");
        node.putArray("items").addObject().put("id", 1001);
        return node;
    }

    @Override
    public JsonNode listReturns(String token) {
        lists.incrementAndGet();
        return JsonNodeFactory.instance.arrayNode();
    }

    @Override
    public JsonNode applyReturn(String token, String orderSn, Map<String, Object> body) {
        applies.incrementAndGet();
        lastApply = body;
        return JsonNodeFactory.instance.numberNode(9001);
    }

    @Override
    public JsonNode cancelOrder(String token, String orderSn) {
        cancels.incrementAndGet();
        if (!ownerToken.equals(token) || !this.orderSn.equals(orderSn)) {
            throw ToolExecutionException.booth(ToolErrorKind.REJECTED, 30201, "订单不存在");
        }
        if (!"WAIT_PAY".equals(status)) {
            throw ToolExecutionException.booth(ToolErrorKind.REJECTED, 30601, "只有待付款订单可以取消");
        }
        status = "CLOSED";
        return null;
    }

    @Override
    public JsonNode updateReceiver(String token, String orderSn, Map<String, Object> body) {
        receiverUpdates.incrementAndGet();
        lastReceiver = body;
        if (!ownerToken.equals(token) || !this.orderSn.equals(orderSn)) {
            throw ToolExecutionException.booth(ToolErrorKind.REJECTED, 30201, "订单不存在");
        }
        if (!"WAIT_DELIVER".equals(status)) {
            throw ToolExecutionException.booth(ToolErrorKind.REJECTED, 30206, "只有待发货订单可以修改收货地址");
        }
        return null;
    }
}
