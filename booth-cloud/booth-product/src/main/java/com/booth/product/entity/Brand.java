package com.booth.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.booth.common.mybatis.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("brand")
public class Brand extends BaseEntity {

    private String name;
    private String firstLetter;
    private String logo;
    private String story;
    private Integer sort;
    private Boolean showStatus;
}
