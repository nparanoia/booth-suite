package com.booth.product.dto;

import com.booth.common.mybatis.page.PageQuery;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BrandQuery extends PageQuery {

    private String keyword;
}
