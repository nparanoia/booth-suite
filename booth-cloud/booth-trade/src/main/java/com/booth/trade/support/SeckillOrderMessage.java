package com.booth.trade.support;

/**
 * 订单号在发送前就生成，消费重试必须用同一个号，不能每次重新取号。
 */
public record SeckillOrderMessage(
        String requestId,
        String orderSn,
        long orderId,
        long memberId,
        long seckillSkuId,
        int quantity,
        long addressId) {
}
