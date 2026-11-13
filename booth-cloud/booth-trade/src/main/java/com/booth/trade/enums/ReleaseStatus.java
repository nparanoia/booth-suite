package com.booth.trade.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 优惠券模板、秒杀活动共用的发布状态。
 */
@Getter
@RequiredArgsConstructor
public enum ReleaseStatus {

    DRAFT(0, "未发布"),
    RELEASED(1, "已发布"),
    OFFLINE(2, "已下线");

    @EnumValue
    private final int code;
    private final String desc;
}
