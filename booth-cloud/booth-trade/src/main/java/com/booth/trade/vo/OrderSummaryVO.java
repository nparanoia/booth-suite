package com.booth.trade.vo;

import com.booth.trade.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderSummaryVO(
        String orderSn,
        OrderStatus status,
        BigDecimal payAmount,
        LocalDateTime createTime) {
}
