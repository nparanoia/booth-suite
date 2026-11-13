package com.booth.api.product.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * @param availableStock 可售库存 = 实物库存 - 锁定库存
 * @param onSale         商品已上架且未删除
 */
public record SkuInfoDTO(
        Long skuId,
        Long productId,
        String skuCode,
        String productName,
        String pic,
        List<SkuSpec> specs,
        BigDecimal price,
        int availableStock,
        boolean onSale,
        Long categoryId,
        String brandName,
        int giftIntegration) {
}
