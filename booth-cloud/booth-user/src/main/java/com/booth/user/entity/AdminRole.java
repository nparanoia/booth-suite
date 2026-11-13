package com.booth.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.booth.common.mybatis.entity.BaseEntity;
import com.booth.user.enums.AccountStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("admin_role")
public class AdminRole extends BaseEntity {

    private String code;
    private String name;
    private String description;
    private AccountStatus status;
    private Integer sort;
}
