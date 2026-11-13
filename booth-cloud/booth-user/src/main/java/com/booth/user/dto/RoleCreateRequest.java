package com.booth.user.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RoleCreateRequest(
        @NotBlank @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,63}$", message = "角色编码为大写字母开头的大写字母、数字或下划线") String code,
        @NotBlank @Size(max = 64) String name,
        @Size(max = 200) String description,
        @Min(0) Integer sort) {
}
