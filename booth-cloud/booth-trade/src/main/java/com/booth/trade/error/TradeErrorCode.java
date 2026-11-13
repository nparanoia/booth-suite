package com.booth.trade.error;

import com.booth.common.core.exception.ErrorCode;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum TradeErrorCode implements ErrorCode {

    SKU_NOT_FOUND(30001, "商品不存在"),
    SKU_OFF_SHELF(30002, "商品已下架"),
    STOCK_NOT_ENOUGH(30003, "库存不足"),

    CART_ITEM_NOT_FOUND(30101, "购物车里没有这件商品"),
    CART_FULL(30102, "购物车最多放 100 种商品"),

    ORDER_NOT_FOUND(30201, "订单不存在"),
    ORDER_STATUS_CHANGED(30202, "订单状态已变化"),
    ORDER_TOKEN_INVALID(30203, "请勿重复提交"),
    ORDER_ITEM_EMPTY(30204, "没有可结算的商品"),
    ORDER_PAY_TOO_SMALL(30205, "优惠后应付金额不能小于 0.01 元"),
    ORDER_RECEIVER_LOCKED(30206, "只有待发货订单可以修改收货地址"),

    PAY_SIGN_INVALID(30301, "支付通知验签失败"),
    PAY_AMOUNT_MISMATCH(30302, "支付金额与订单不一致"),
    PAY_CHANNEL_DISABLED(30303, "未配置支付渠道"),

    COUPON_NOT_FOUND(30401, "优惠券不存在"),
    COUPON_UNAVAILABLE(30402, "优惠券不可用"),
    COUPON_SOLD_OUT(30403, "优惠券已领完"),
    COUPON_LIMIT(30404, "已达到每人限领数量"),
    COUPON_NOT_APPLICABLE(30405, "优惠券不满足使用条件"),
    COUPON_IMMUTABLE(30406, "已发布的优惠券不能修改面额和门槛"),
    COUPON_DELETE(30407, "只有草稿可以删除"),

    SECKILL_NOT_FOUND(30501, "秒杀商品不存在"),
    SECKILL_NOT_STARTED(30502, "秒杀尚未开始"),
    SECKILL_PATH_INVALID(30503, "抢购地址无效或已使用"),
    SECKILL_LIMIT(30504, "超过每人限购数量"),
    SECKILL_SOLD_OUT(30505, "秒杀库存不足"),
    SECKILL_STOCK_INVALID(30506, "秒杀库存不能超过当前可售库存"),
    SECKILL_IMMUTABLE(30507, "已发布或已预热的秒杀不能这样修改"),
    SECKILL_BUSY(30508, "抢购人数太多，请稍后再试"),
    SECKILL_QUEUE_FAILED(30509, "抢购请求没能送出，请重试"),

    RETURN_NOT_FOUND(30601, "售后申请不存在"),
    RETURN_EXISTS(30602, "该商品已申请过售后"),
    RETURN_STATUS_CHANGED(30603, "售后状态已变化"),
    RETURN_ORDER_INVALID(30604, "当前订单不能申请售后");

    private final int code;
    private final String message;

    @Override
    public int code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
