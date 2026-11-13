package com.booth.trade.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.booth.common.mybatis.entity.BaseCreateEntity;
import com.booth.trade.enums.SeckillRecordStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("seckill_order")
public class SeckillOrder extends BaseCreateEntity {

    private Long seckillSkuId;
    private Long memberId;
    private String orderSn;
    private Integer quantity;
    /** 关闭后置为已关闭，会员才能再抢。不能用 updateById 改。 */
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private SeckillRecordStatus status;
    /**
     * 有效单等于 member_id，关闭后置空。
     * 唯一键加在这一列上：MySQL 里多个 NULL 可以共存，已关闭的记录不再挡住同一个人。
     */
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private Long activeMember;
}
