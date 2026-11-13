package com.booth.trade.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SeckillRecordStatus {

    ACTIVE(0, "有效"),
    CLOSED(1, "已关闭");

    @EnumValue
    private final int code;
    private final String desc;
}
