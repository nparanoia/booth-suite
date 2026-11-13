package com.booth.api.product.dto;

/**
 * SKU 销售规格的一项，如 key=颜色、value=黑色。商品库和订单明细都以 JSON 数组存储。
 */
public record SkuSpec(String key, String value) {
}
