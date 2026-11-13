package com.booth.product.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AttrType {

    /** 销售规格，组合出 SKU，如颜色、容量 */
    SALE_SPEC(0, "销售规格"),
    /** 展示参数，挂在商品上，如屏幕尺寸、CPU */
    PARAM(1, "展示参数");

    @EnumValue
    private final int code;
    private final String desc;
}
