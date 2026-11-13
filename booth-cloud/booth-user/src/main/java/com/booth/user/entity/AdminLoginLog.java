package com.booth.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.booth.common.mybatis.entity.BaseCreateEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("admin_login_log")
public class AdminLoginLog extends BaseCreateEntity {

    private Long adminId;
    private String ip;
    private String userAgent;
    private Boolean success;
}
