package com.booth.trade.vo;

import com.booth.api.product.dto.SkuSpec;

import java.math.BigDecimal;
import java.util.List;

public record OrderItemVO(
        Long skuId,
        String productName,
        String pic,
        List<SkuSpec> specs,
        BigDecimal price,
        Integer quantity,
        BigDecimal realAmount,
        Long id) {
}
