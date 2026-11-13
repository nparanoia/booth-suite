package com.booth.product.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.booth.api.product.dto.SkuSpec;
import com.booth.common.mybatis.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * stock、lockStock、sale 会被下单并发修改，不参与 updateById，避免把读到的旧值写回去；
 * 它们只能通过带条件的 UPDATE 修改。
 */
@Getter
@Setter
@TableName(value = "sku", autoResultMap = true)
public class Sku extends BaseEntity {

    private Long productId;
    private String skuCode;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<SkuSpec> specData;
    private BigDecimal price;
    private String pic;
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private Integer stock;
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private Integer lockStock;
    private Integer lowStock;
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private Integer sale;

    public int availableStock() {
        return stock - lockStock;
    }
}
