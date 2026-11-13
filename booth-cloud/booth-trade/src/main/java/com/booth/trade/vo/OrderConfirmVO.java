package com.booth.trade.vo;

import java.math.BigDecimal;
import java.util.List;

public record OrderConfirmVO(
        String submitToken,
        List<OrderItemVO> items,
        List<CouponCandidateVO> coupons,
        int integration,
        int maxUseIntegration,
        BigDecimal goodsAmount,
        BigDecimal freightAmount,
        BigDecimal couponAmount,
        BigDecimal integrationAmount,
        BigDecimal payAmount,
        int useIntegration) {
}
