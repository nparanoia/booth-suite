package com.booth.trade.vo;

import java.math.BigDecimal;

public record CouponCandidateVO(
        Long memberCouponId,
        Long couponId,
        String name,
        BigDecimal amount,
        BigDecimal minPoint,
        boolean usable) {
}
