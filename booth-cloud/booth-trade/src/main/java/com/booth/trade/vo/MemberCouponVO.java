package com.booth.trade.vo;

import com.booth.trade.enums.MemberCouponStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MemberCouponVO(
        Long id,
        Long couponId,
        String name,
        BigDecimal amount,
        BigDecimal minPoint,
        MemberCouponStatus status,
        LocalDateTime useEndTime) {
}
