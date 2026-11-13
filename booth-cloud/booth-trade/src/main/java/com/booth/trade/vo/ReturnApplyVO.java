package com.booth.trade.vo;

import com.booth.trade.enums.ReturnStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReturnApplyVO(
        Long id,
        String orderSn,
        Long orderItemId,
        Integer returnQuantity,
        BigDecimal returnAmount,
        String reason,
        String description,
        ReturnStatus status,
        String handleNote,
        LocalDateTime createTime) {
}
