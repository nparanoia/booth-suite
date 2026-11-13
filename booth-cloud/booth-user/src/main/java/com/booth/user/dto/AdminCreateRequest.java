package com.booth.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminCreateRequest(
        @NotBlank @Pattern(regexp = "^[a-zA-Z0-9_]{4,32}$", message = "用户名为 4-32 位字母、数字或下划线") String username,
        @NotBlank @Size(min = 8, max = 32, message = "管理员密码长度为 8-32 位") String password,
        @Size(max = 64) String nickname,
        @Email @Size(max = 100) String email) {
}
