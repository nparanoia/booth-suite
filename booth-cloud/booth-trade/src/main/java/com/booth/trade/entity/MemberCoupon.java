package com.booth.trade.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.booth.common.mybatis.entity.BaseEntity;
import com.booth.trade.enums.MemberCouponStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("member_coupon")
public class MemberCoupon extends BaseEntity {

    private Long couponId;
    private Long memberId;
    /** 0 后台发放，1 主动领取 */
    private Integer getType;
    /** 锁定、核销、释放都走 CAS 条件更新 */
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private MemberCouponStatus status;
    private String orderSn;
    private LocalDateTime lockTime;
    private LocalDateTime useTime;
}
