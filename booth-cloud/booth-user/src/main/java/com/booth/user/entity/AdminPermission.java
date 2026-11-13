package com.booth.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.booth.common.mybatis.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("admin_permission")
public class AdminPermission extends BaseEntity {

    private Long parentId;
    /** 与接口上的权限注解一一对应，如 product:spu:update */
    private String code;
    private String name;
    /** 1 模块，2 操作 */
    private Integer type;
    private Integer sort;
}
