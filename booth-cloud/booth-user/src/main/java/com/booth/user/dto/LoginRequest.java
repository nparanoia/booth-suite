package com.booth.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 会员和管理员登录共用。密码上限与注册一致，同时避开 BCrypt 只处理前 72 字节的限制。
 */
public record LoginRequest(
        @NotBlank(message = "用户名不能为空") @Size(max = 64) String username,
        @NotBlank(message = "密码不能为空") @Size(max = 32) String password) {
}
