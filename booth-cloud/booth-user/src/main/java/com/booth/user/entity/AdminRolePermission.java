package com.booth.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.booth.common.mybatis.entity.BaseCreateEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("admin_role_permission")
public class AdminRolePermission extends BaseCreateEntity {

    private Long roleId;
    private Long permissionId;
}
