package com.booth.trade.vo;

import com.booth.api.product.dto.SkuSpec;

import java.math.BigDecimal;
import java.util.List;

/**
 * @param checkedAmount 只统计已勾选且可购买的商品，价格来自商品服务的实时价格
 */
public record CartVO(List<Item> items, int checkedCount, BigDecimal checkedAmount) {

    /**
     * @param available 商品存在且已上架；为 false 时前端展示为失效商品
     */
    public record Item(
            Long id,
            Long skuId,
            Long productId,
            String productName,
            String pic,
            List<SkuSpec> specs,
            BigDecimal price,
            int quantity,
            boolean checked,
            boolean available,
            int availableStock) {
    }
}
