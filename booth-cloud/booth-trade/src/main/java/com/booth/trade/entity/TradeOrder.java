package com.booth.trade.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.booth.trade.enums.OrderStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 主键用雪花 ID，不能继承自增主键的 BaseEntity。
 * status 只能通过 CAS 条件更新修改，不参与 updateById。
 */
@Getter
@Setter
@TableName("trade_order")
public class TradeOrder {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String orderSn;
    private Long memberId;
    private String memberUsername;
    /** 0 普通，1 秒杀 */
    private Integer orderType;
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private OrderStatus status;
    private BigDecimal totalAmount;
    private BigDecimal freightAmount;
    private BigDecimal couponAmount;
    private BigDecimal integrationAmount;
    private BigDecimal payAmount;
    private Long memberCouponId;
    private Integer useIntegration;
    private Integer rewardIntegration;
    /** 1 支付宝，2 微信 */
    private Integer payType;
    private LocalDateTime paymentTime;
    private String receiverName;
    private String receiverPhone;
    private String receiverProvince;
    private String receiverCity;
    private String receiverRegion;
    private String receiverDetailAddress;
    private String receiverPostCode;
    private String note;
    private String deliveryCompany;
    private String deliverySn;
    private LocalDateTime deliveryTime;
    private LocalDateTime receiveTime;
    private LocalDateTime closeTime;
    /** TIMEOUT / MEMBER_CANCEL / ADMIN_CLOSE */
    private String closeReason;
    private Boolean memberDeleted;
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createTime;
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updateTime;
}
