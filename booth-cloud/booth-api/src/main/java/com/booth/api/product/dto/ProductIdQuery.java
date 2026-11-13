package com.booth.api.product.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 品牌或分类改名后，找出挂在它们下面的商品。两个列表可以只填一个。
 */
public record ProductIdQuery(
        @Size(max = 100) List<Long> brandIds,
        @Size(max = 100) List<Long> categoryIds) {
}
