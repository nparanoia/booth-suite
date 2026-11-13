package com.booth.user.vo;

import com.booth.user.enums.AccountStatus;
import com.booth.user.enums.Gender;

import java.time.LocalDateTime;

public record MemberAdminVO(
        Long id,
        String username,
        String nickname,
        String phone,
        Gender gender,
        Integer integration,
        AccountStatus status,
        LocalDateTime createTime) {
}
