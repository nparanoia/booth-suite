package com.booth.trade.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.booth.common.mybatis.entity.BaseEntity;
import com.booth.trade.enums.PaymentStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("payment_record")
public class PaymentRecord extends BaseEntity {

    private String orderSn;
    private Integer payType;
    private BigDecimal amount;
    private String channelTradeNo;
    /** 只能按状态做条件更新 */
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private PaymentStatus status;
    private LocalDateTime notifyTime;
}
