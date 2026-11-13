package com.booth.user.error;

import com.booth.common.core.exception.ErrorCode;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

    MEMBER_NOT_FOUND(10001, "会员不存在"),
    USERNAME_EXISTS(10002, "用户名已被注册"),
    PHONE_EXISTS(10003, "手机号已被注册"),
    MEMBER_DISABLED(10004, "账号已被禁用"),
    LOGIN_FAILED(10005, "用户名或密码错误"),
    LOGIN_RATE_LIMITED(10006, "登录尝试过于频繁，请 15 分钟后再试"),

    ADDRESS_NOT_FOUND(10101, "收货地址不存在"),
    ADDRESS_LIMIT(10102, "最多只能保存 20 个收货地址"),

    ADMIN_NOT_FOUND(10201, "管理员不存在"),
    ADMIN_USERNAME_EXISTS(10202, "管理员用户名已存在"),
    ADMIN_DISABLED(10203, "管理员账号已被禁用"),
    CANNOT_DISABLE_SELF(10204, "不能禁用当前登录的账号"),
    ROLE_NOT_FOUND(10211, "角色不存在"),
    ROLE_CODE_EXISTS(10212, "角色编码已存在"),
    SUPER_ROLE_READONLY(10213, "超级管理员角色拥有全部权限，不能修改权限或禁用"),
    PERMISSION_NOT_FOUND(10221, "权限不存在"),

    INTEGRATION_NOT_ENOUGH(10301, "积分不足"),
    INTEGRATION_BIZ_INVALID(10302, "没有可回退的积分扣减");

    private final int code;
    private final String message;

    @Override
    public int code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
