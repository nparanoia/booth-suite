package com.booth.trade.support;

public final class SeckillMq {

    public static final String EXCHANGE = "booth.seckill";
    public static final String QUEUE = "booth.seckill.order";
    public static final String ROUTING_KEY = "seckill.order";
    public static final String BIZ_ORDER = "SECKILL_ORDER";

    private SeckillMq() {
    }
}
