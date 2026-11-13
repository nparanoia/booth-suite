package com.booth.product.vo;

import com.booth.product.enums.PublishStatus;

import java.math.BigDecimal;

public record ProductListVO(
        Long id,
        String productSn,
        String name,
        String subTitle,
        String pic,
        BigDecimal price,
        BigDecimal originalPrice,
        Integer sale,
        PublishStatus publishStatus,
        String brandName,
        String categoryName) {
}
