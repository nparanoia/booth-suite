package com.booth.api.product.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 搜索索引里的一条商品。未上架或已删除时 onShelf 为 false，只带 id 和版本，用来从索引里删掉。
 */
public record ProductSearchDocDTO(
        Long id,
        boolean onShelf,
        long version,
        String name,
        String subTitle,
        String keywords,
        Long brandId,
        Long categoryId,
        String brandName,
        String categoryName,
        BigDecimal price,
        Integer sale,
        String pic,
        Long createTime,
        List<AttrDTO> attrs) {

    public record AttrDTO(String name, String value) {
    }
}
