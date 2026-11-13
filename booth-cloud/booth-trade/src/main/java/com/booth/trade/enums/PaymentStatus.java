package com.booth.trade.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentStatus {

    WAIT_PAY(0, "待支付"),
    SUCCESS(1, "支付成功"),
    CLOSED(2, "已关闭"),
    /** 订单已关闭后收到支付成功通知，钱还在渠道里，需要退款 */
    REFUND_REQUIRED(3, "关闭后到账待退款");

    @EnumValue
    private final int code;
    private final String desc;
}
