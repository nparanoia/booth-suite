package com.booth.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.booth.common.mybatis.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("product_attr_value")
public class ProductAttrValue extends BaseEntity {

    private Long productId;
    private Long attrId;
    private String value;
}
