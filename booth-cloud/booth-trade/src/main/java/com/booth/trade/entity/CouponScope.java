package com.booth.trade.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.booth.common.mybatis.entity.BaseCreateEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("coupon_scope")
public class CouponScope extends BaseCreateEntity {

    private Long couponId;
    /** 1 分类，2 商品 */
    private Integer scopeType;
    private Long targetId;
    private String targetName;
}
