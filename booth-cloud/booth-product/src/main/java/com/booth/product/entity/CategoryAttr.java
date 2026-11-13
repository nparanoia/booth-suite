package com.booth.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.booth.common.mybatis.entity.BaseCreateEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("category_attr")
public class CategoryAttr extends BaseCreateEntity {

    private Long categoryId;
    private Long attrId;
}
