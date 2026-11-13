package com.booth.user.vo;

import com.booth.user.enums.AccountStatus;

public record RoleVO(
        Long id,
        String code,
        String name,
        String description,
        AccountStatus status,
        Integer sort) {
}
