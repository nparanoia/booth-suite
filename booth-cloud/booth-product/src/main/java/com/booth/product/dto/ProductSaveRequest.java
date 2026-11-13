package com.booth.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * 新增和修改共用的商品基础信息。SKU 单独维护，修改商品不会动已有 SKU 的库存。
 */
public record ProductSaveRequest(
        @NotNull(message = "分类不能为空") Long categoryId,
        @NotNull(message = "品牌不能为空") Long brandId,
        Long attrGroupId,
        @NotBlank(message = "货号不能为空") @Size(max = 64) String productSn,
        @NotBlank(message = "商品名称不能为空") @Size(max = 200) String name,
        @Size(max = 255) String subTitle,
        @Size(max = 500) String pic,
        @Size(max = 20) List<String> albumPics,
        @Size(max = 255) String keywords,
        String detailHtml,
        @DecimalMin("0") BigDecimal originalPrice,
        @Size(max = 16) String unit,
        @DecimalMin("0") BigDecimal weight,
        Integer sort,
        Boolean newStatus,
        Boolean recommendStatus,
        @Min(0) Integer giftIntegration,
        @Valid List<ParamValue> params) {

    public record ParamValue(
            @NotNull Long attrId,
            @NotBlank @Size(max = 500) String value) {
    }
}
