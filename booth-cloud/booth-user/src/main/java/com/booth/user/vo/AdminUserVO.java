package com.booth.user.vo;

import com.booth.user.enums.AccountStatus;

import java.time.LocalDateTime;
import java.util.List;

public record AdminUserVO(
        Long id,
        String username,
        String nickname,
        String email,
        AccountStatus status,
        LocalDateTime lastLoginTime,
        LocalDateTime createTime,
        List<Long> roleIds) {
}
