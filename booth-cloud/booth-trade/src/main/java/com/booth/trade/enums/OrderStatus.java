package com.booth.trade.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 待付款 -> 待发货 -> 已发货 -> 已完成；待付款可关闭。状态流转统一用 WHERE status = ? 的条件更新。
 */
@Getter
@RequiredArgsConstructor
public enum OrderStatus {

    WAIT_PAY(0, "待付款"),
    WAIT_DELIVER(1, "待发货"),
    DELIVERED(2, "已发货"),
    COMPLETED(3, "已完成"),
    CLOSED(4, "已关闭");

    @EnumValue
    private final int code;
    private final String desc;
}
