package com.booth.trade.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.booth.common.mybatis.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@TableName("seckill_sku")
public class SeckillSku extends BaseEntity {

    private Long activityId;
    private Long sessionId;
    private Long productId;
    private Long skuId;
    private String productName;
    private String productPic;
    private BigDecimal seckillPrice;
    /** 活动开始前预热到 Redis，异步下单时用条件更新扣减 */
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private Integer seckillStock;
    /** 创建时的活动库存。预热前若修改库存会一起改，预热后不再改。对账用它减去剩余库存。 */
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private Integer seckillStockInitial;
    private Integer perLimit;
    private Integer sort;
}
