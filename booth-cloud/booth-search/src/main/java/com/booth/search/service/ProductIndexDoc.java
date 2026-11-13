package com.booth.search.service;

import com.booth.api.product.dto.ProductSearchDocDTO;

import java.math.BigDecimal;
import java.util.List;

/**
 * 写入 Elasticsearch 的文档。版本号放在索引的外部版本上，updateTime 留一份方便排查。
 */
public record ProductIndexDoc(
        long id,
        String name,
        String subTitle,
        String keywords,
        long brandId,
        long categoryId,
        String brandName,
        String categoryName,
        BigDecimal price,
        int sale,
        String pic,
        List<Attr> attrs,
        long createTime,
        long updateTime) {

    public record Attr(String name, String value) {
    }

    static ProductIndexDoc from(ProductSearchDocDTO doc) {
        List<Attr> attrs = doc.attrs() == null ? List.of()
                : doc.attrs().stream().map(attr -> new Attr(attr.name(), attr.value())).toList();
        long createTime = doc.createTime() == null ? doc.version() : doc.createTime();
        return new ProductIndexDoc(doc.id(), doc.name(), doc.subTitle(), doc.keywords(),
                doc.brandId(), doc.categoryId(), doc.brandName(), doc.categoryName(),
                doc.price(), doc.sale() == null ? 0 : doc.sale(), doc.pic(), attrs, createTime, doc.version());
    }
}
