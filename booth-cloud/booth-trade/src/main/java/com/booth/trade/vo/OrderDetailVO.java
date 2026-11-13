package com.booth.trade.vo;

import com.booth.trade.enums.OrderStatus;
import com.booth.trade.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderDetailVO(
        String orderSn,
        Long memberId,
        String memberUsername,
        OrderStatus status,
        Integer orderType,
        BigDecimal totalAmount,
        BigDecimal freightAmount,
        BigDecimal couponAmount,
        BigDecimal integrationAmount,
        BigDecimal payAmount,
        Integer useIntegration,
        Integer rewardIntegration,
        Integer payType,
        PaymentStatus paymentStatus,
        LocalDateTime paymentTime,
        String receiverName,
        String receiverPhone,
        String receiverProvince,
        String receiverCity,
        String receiverRegion,
        String receiverDetailAddress,
        String note,
        String deliveryCompany,
        String deliverySn,
        LocalDateTime deliveryTime,
        LocalDateTime receiveTime,
        LocalDateTime closeTime,
        String closeReason,
        LocalDateTime createTime,
        List<OrderItemVO> items) {
}
