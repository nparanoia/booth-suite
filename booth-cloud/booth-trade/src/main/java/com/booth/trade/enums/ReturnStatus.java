package com.booth.trade.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReturnStatus {

    PENDING(0, "待处理"),
    RETURNING(1, "退货中"),
    COMPLETED(2, "已完成"),
    REJECTED(3, "已拒绝");

    @EnumValue
    private final int code;
    private final String desc;
}
