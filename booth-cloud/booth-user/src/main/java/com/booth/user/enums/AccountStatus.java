package com.booth.user.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AccountStatus {

    DISABLED(0, "禁用"),
    ENABLED(1, "启用");

    @EnumValue
    private final int code;
    private final String desc;
}
