package com.booth.trade.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.booth.common.mybatis.entity.BaseEntity;
import com.booth.trade.enums.ReleaseStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("coupon")
public class Coupon extends BaseEntity {

    private String name;
    private BigDecimal amount;
    private BigDecimal minPoint;
    /** 0 全场通用，1 指定分类，2 指定商品 */
    private Integer useType;
    private Integer totalCount;
    /** 领券并发扣减，只能用 remain_count > 0 的条件更新 */
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private Integer remainCount;
    private Integer perLimit;
    private LocalDateTime receiveStartTime;
    private LocalDateTime receiveEndTime;
    private LocalDateTime useStartTime;
    private LocalDateTime useEndTime;
    private ReleaseStatus status;
    private String note;
}
