package com.booth.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.booth.common.mybatis.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("category")
public class Category extends BaseEntity {

    public static final long ROOT_ID = 0L;
    public static final int MAX_LEVEL = 2;

    private Long parentId;
    private String name;
    private Integer level;
    private String icon;
    private String keywords;
    private Integer sort;
    private Boolean navStatus;
    private Boolean showStatus;
}
