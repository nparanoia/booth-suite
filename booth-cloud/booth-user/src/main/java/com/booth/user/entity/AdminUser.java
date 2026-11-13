package com.booth.user.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.booth.common.mybatis.entity.BaseEntity;
import com.booth.user.enums.AccountStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("admin_user")
public class AdminUser extends BaseEntity {

    private String username;
    private String password;
    private String nickname;
    private String email;
    private String avatar;
    private AccountStatus status;
    private LocalDateTime lastLoginTime;
    private String note;
    @TableLogic
    private Integer deleted;
}
