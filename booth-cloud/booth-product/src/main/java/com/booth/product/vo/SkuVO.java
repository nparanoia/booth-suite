package com.booth.product.vo;

import com.booth.api.product.dto.SkuSpec;

import java.math.BigDecimal;
import java.util.List;

/**
 * stock、lockStock、lowStock 只在后台返回；前台构造时置为 null，借助 non_null 序列化不输出。
 */
public record SkuVO(
        Long id,
        String skuCode,
        List<SkuSpec> specs,
        BigDecimal price,
        String pic,
        Integer availableStock,
        Integer stock,
        Integer lockStock,
        Integer lowStock,
        Integer sale) {

    public SkuVO forOpen() {
        return new SkuVO(id, skuCode, specs, price, pic, availableStock, null, null, null, sale);
    }

    public SkuVO withStock(int availableStock, int sale) {
        return new SkuVO(id, skuCode, specs, price, pic, availableStock, null, null, null, sale);
    }
}
