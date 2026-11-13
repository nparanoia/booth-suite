package com.booth.trade.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.booth.common.mybatis.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * 购物车不存价格和商品名，展示时实时查商品服务，避免显示过期价格。
 */
@Getter
@Setter
@TableName("cart_item")
public class CartItem extends BaseEntity {

    private Long memberId;
    private Long productId;
    private Long skuId;
    private Integer quantity;
    private Boolean checked;
}
