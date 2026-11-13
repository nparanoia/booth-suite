package com.booth.trade.vo;

import java.math.BigDecimal;

public record SeckillSkuVO(
        Long id,
        Long activityId,
        Long sessionId,
        Long skuId,
        String productName,
        String productPic,
        BigDecimal seckillPrice,
        Integer seckillStock,
        Integer remainStock,
        Integer perLimit,
        Integer sort) {
}
