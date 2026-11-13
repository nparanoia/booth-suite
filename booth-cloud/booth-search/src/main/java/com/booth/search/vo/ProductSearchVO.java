package com.booth.search.vo;

import java.math.BigDecimal;
import java.util.List;

public record ProductSearchVO(
        long total,
        long pageNum,
        long pageSize,
        List<Item> list,
        List<Bucket> brands,
        List<Bucket> categories,
        List<AttrBucket> attrs) {

    public record Item(
            Long id,
            String name,
            String highlightName,
            String subTitle,
            String pic,
            BigDecimal price,
            Integer sale,
            String brandName,
            String categoryName) {
    }

    public record Bucket(Long id, String name, long count) {
    }

    public record AttrBucket(String name, List<ValueBucket> values) {
    }

    public record ValueBucket(String value, long count) {
    }
}
