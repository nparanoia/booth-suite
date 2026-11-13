package com.booth.trade.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LocalMessageStatus {

    PENDING(0, "待确认"),
    CONFIRMED(1, "已确认"),
    FAILED(2, "超过重试上限"),
    ROUTE_FAILED(3, "无法路由");

    @EnumValue
    private final int code;
    private final String desc;
}
