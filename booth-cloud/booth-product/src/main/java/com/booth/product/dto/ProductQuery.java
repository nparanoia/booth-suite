package com.booth.product.dto;

import com.booth.common.mybatis.page.PageQuery;
import com.booth.product.enums.PublishStatus;
import lombok.Getter;
import lombok.Setter;

/**
 * 前台查询时 publishStatus 由服务端固定为上架，客户端传入的值会被忽略。
 */
@Getter
@Setter
public class ProductQuery extends PageQuery {

    private String keyword;
    private Long categoryId;
    private Long brandId;
    private PublishStatus publishStatus;
}
