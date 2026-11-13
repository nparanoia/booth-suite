package com.booth.user.vo;

import com.booth.user.enums.Gender;

import java.time.LocalDate;

/**
 * 会员本人看到的资料，手机号脱敏。
 */
public record MemberProfileVO(
        Long id,
        String username,
        String nickname,
        String phone,
        String avatar,
        Gender gender,
        LocalDate birthday,
        Integer integration) {
}
