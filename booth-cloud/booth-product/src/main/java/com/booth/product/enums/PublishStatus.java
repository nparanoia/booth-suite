package com.booth.product.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PublishStatus {

    OFF_SHELF(0, "下架"),
    ON_SHELF(1, "上架");

    @EnumValue
    private final int code;
    private final String desc;
}
