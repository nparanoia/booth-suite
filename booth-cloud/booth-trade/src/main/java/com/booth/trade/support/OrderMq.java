package com.booth.trade.support;

public final class OrderMq {

    public static final String DELAY_EXCHANGE = "booth.order.delay";
    public static final String EVENT_EXCHANGE = "booth.order.event";
    public static final String DLX = "booth.order.dlx";

    public static final String TIMEOUT_QUEUE = "booth.order.timeout";
    public static final String PAID_QUEUE = "booth.order.paid";
    public static final String CLOSED_QUEUE = "booth.order.closed";
    public static final String COMPLETED_QUEUE = "booth.order.completed";

    public static final String RK_TIMEOUT = "order.timeout";
    public static final String RK_PAID = "order.paid";
    public static final String RK_CLOSED = "order.closed";
    public static final String RK_COMPLETED = "order.completed";

    public static final String BIZ_TIMEOUT = "ORDER_TIMEOUT";
    public static final String BIZ_PAID = "ORDER_PAID";
    public static final String BIZ_CLOSED = "ORDER_CLOSED";
    public static final String BIZ_COMPLETED = "ORDER_COMPLETED";

    private OrderMq() {
    }
}
