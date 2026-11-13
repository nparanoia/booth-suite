package com.booth.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.booth.common.mybatis.entity.BaseCreateEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("admin_user_role")
public class AdminUserRole extends BaseCreateEntity {

    private Long adminId;
    private Long roleId;
}
