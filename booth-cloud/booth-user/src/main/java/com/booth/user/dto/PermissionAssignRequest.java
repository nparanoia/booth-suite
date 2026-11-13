package com.booth.user.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * 全量覆盖：传入的集合就是分配后的结果，传空集合表示清空。
 */
public record PermissionAssignRequest(@NotNull @Size(max = 500) Set<@NotNull Long> permissionIds) {
}
