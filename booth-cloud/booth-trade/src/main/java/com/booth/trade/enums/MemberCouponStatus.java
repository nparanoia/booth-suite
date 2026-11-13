package com.booth.trade.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 与库存同构：下单锁定，支付核销，取消释放回未使用。
 */
@Getter
@RequiredArgsConstructor
public enum MemberCouponStatus {

    UNUSED(0, "未使用"),
    LOCKED(1, "已锁定"),
    USED(2, "已使用"),
    EXPIRED(3, "已过期");

    @EnumValue
    private final int code;
    private final String desc;
}
