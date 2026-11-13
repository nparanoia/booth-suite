package com.booth.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.booth.common.mybatis.entity.BaseEntity;
import com.booth.product.enums.AttrType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("attr")
public class Attr extends BaseEntity {

    private Long groupId;
    private String name;
    private AttrType type;
    /** 0 手工录入，1 从 inputList 中选择 */
    private Integer inputType;
    private String inputList;
    /** 0 不检索，1 关键字，2 范围 */
    private Integer searchType;
    private Integer sort;
}
