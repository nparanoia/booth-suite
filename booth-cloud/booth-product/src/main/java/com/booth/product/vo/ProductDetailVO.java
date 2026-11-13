package com.booth.product.vo;

import com.booth.product.enums.PublishStatus;

import java.math.BigDecimal;
import java.util.List;

public record ProductDetailVO(
        Long id,
        Long categoryId,
        Long brandId,
        Long attrGroupId,
        String productSn,
        String name,
        String subTitle,
        String pic,
        List<String> albumPics,
        String keywords,
        String detailHtml,
        BigDecimal price,
        BigDecimal originalPrice,
        String unit,
        BigDecimal weight,
        Integer sale,
        Integer sort,
        PublishStatus publishStatus,
        Boolean newStatus,
        Boolean recommendStatus,
        Integer giftIntegration,
        String brandName,
        String categoryName,
        List<ParamVO> params,
        List<SkuVO> skus) {

    public ProductDetailVO fresh(Integer sale, List<SkuVO> skus) {
        return new ProductDetailVO(id, categoryId, brandId, attrGroupId, productSn, name, subTitle, pic, albumPics,
                keywords, detailHtml, price, originalPrice, unit, weight, sale, sort, publishStatus, newStatus,
                recommendStatus, giftIntegration, brandName, categoryName, params, skus);
    }

    public record ParamVO(Long attrId, String name, String value) {
    }
}
