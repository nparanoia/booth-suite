package com.booth.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.booth.common.mybatis.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("attr_group")
public class AttrGroup extends BaseEntity {

    private String name;
}
