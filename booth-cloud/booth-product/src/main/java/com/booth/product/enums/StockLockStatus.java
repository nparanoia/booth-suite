package com.booth.product.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StockLockStatus {

    LOCKED(1, "已锁定"),
    DEDUCTED(2, "已扣减"),
    RELEASED(3, "已释放");

    @EnumValue
    private final int code;
    private final String desc;
}
