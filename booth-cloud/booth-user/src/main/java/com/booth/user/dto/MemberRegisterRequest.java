package com.booth.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MemberRegisterRequest(
        @NotBlank @Pattern(regexp = "^[a-zA-Z0-9_]{4,32}$", message = "用户名为 4-32 位字母、数字或下划线") String username,
        @NotBlank @Size(min = 6, max = 32, message = "密码长度为 6-32 位") String password,
        @NotBlank @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确") String phone,
        @Size(max = 64) String nickname) {
}
