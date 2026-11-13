package com.booth.trade.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.booth.common.mybatis.entity.BaseEntity;
import com.booth.trade.enums.ReturnStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("order_return_apply")
public class OrderReturnApply extends BaseEntity {

    private Long orderId;
    private Long orderItemId;
    private String orderSn;
    private Long memberId;
    private Integer returnQuantity;
    private BigDecimal returnAmount;
    private String reason;
    private String description;
    private String proofPics;
    /** 审批流转用 CAS 条件更新 */
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private ReturnStatus status;
    private String handleAdmin;
    private String handleNote;
    private LocalDateTime handleTime;
    private String receiveAdmin;
    private String receiveNote;
    private LocalDateTime receiveTime;
}
