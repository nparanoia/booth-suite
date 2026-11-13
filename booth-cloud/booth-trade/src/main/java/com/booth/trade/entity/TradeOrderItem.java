package com.booth.trade.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.booth.api.product.dto.SkuSpec;
import com.booth.common.mybatis.entity.BaseCreateEntity;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 下单时的商品快照，之后商品改名、改价都不影响历史订单。
 */
@Getter
@Setter
@TableName(value = "trade_order_item", autoResultMap = true)
public class TradeOrderItem extends BaseCreateEntity {

    private Long orderId;
    private String orderSn;
    private Long productId;
    private Long skuId;
    private String productName;
    private String productPic;
    private String skuCode;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<SkuSpec> specData;
    private String brandName;
    private Long categoryId;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal couponAmount;
    private BigDecimal integrationAmount;
    private BigDecimal realAmount;
}
