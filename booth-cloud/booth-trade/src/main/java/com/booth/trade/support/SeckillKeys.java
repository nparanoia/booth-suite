package com.booth.trade.support;

public final class SeckillKeys {

    private static final String PREFIX = "booth:seckill:";

    private SeckillKeys() {
    }

    public static String stock(long seckillSkuId) {
        return PREFIX + "stock:" + seckillSkuId;
    }

    public static String info(long seckillSkuId) {
        return PREFIX + "info:" + seckillSkuId;
    }

    public static String bought(long seckillSkuId) {
        return PREFIX + "bought:" + seckillSkuId;
    }

    public static String path(long memberId, long seckillSkuId) {
        return PREFIX + "path:" + memberId + ":" + seckillSkuId;
    }

    public static String result(String requestId) {
        return PREFIX + "result:" + requestId;
    }

    public static String abort(String requestId) {
        return PREFIX + "abort:" + requestId;
    }

    /** 消费者正在落单。修复任务看到它就先不归还，避免订单还没写入就把库存加回去。 */
    public static String placing(String requestId) {
        return PREFIX + "placing:" + requestId;
    }

    public static String revert(String token) {
        return PREFIX + "revert:" + token;
    }
}
