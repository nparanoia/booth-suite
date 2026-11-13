package com.booth.trade.support;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * 交易相关的业务计数。指标在进程启动时就注册，没有发生过的事件在 Prometheus 里是 0。
 */
@Component
public class TradeMetrics {

    private final Counter ordersPlaced;
    private final Counter ordersFailed;
    private final Counter seckillLimited;
    private final Counter localMessageRetries;
    private final Counter localMessageExhausted;
    private final Counter reconcileDiffs;
    private final Counter payAfterClose;

    public TradeMetrics(MeterRegistry registry) {
        this.ordersPlaced = counter(registry, "booth.order.placed", "提交订单成功的次数");
        this.ordersFailed = counter(registry, "booth.order.failed", "提交订单失败的次数");
        this.seckillLimited = Counter.builder("booth.seckill.limited")
                .description("秒杀被限流的次数")
                .tag("source", "hotspot")
                .register(registry);
        this.localMessageRetries = counter(registry, "booth.local.message.retries", "本地消息重新投递的次数");
        this.localMessageExhausted = counter(registry, "booth.local.message.exhausted", "本地消息超过重试上限的次数");
        this.reconcileDiffs = counter(registry, "booth.reconcile.diffs", "对账发现差异的次数");
        this.payAfterClose = counter(registry, "booth.pay.after.close", "订单关闭后仍收到付款的次数");
    }

    public void orderPlaced() {
        ordersPlaced.increment();
    }

    public void orderFailed() {
        ordersFailed.increment();
    }

    public void seckillLimited() {
        seckillLimited.increment();
    }

    public void localMessageRetried() {
        localMessageRetries.increment();
    }

    public void localMessageExhausted() {
        localMessageExhausted.increment();
    }

    public void reconcileDiff() {
        reconcileDiffs.increment();
    }

    public void payAfterClose() {
        payAfterClose.increment();
    }

    private static Counter counter(MeterRegistry registry, String name, String description) {
        return Counter.builder(name).description(description).register(registry);
    }
}
