package com.booth.user.dto;

import com.booth.user.enums.AccountStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 角色编码创建后不能修改，代码里可能按编码判断（如 SUPER_ADMIN）。
 */
public record RoleUpdateRequest(
        @NotBlank @Size(max = 64) String name,
        @Size(max = 200) String description,
        @NotNull @Min(0) Integer sort,
        @NotNull AccountStatus status) {
}
