package com.booth.user.dto;

import com.booth.user.enums.Gender;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 只允许改展示信息。用户名、手机号、积分、状态都不在这里，防止客户端顺手改掉。
 */
public record MemberProfileUpdateRequest(
        @Size(max = 64) String nickname,
        @Size(max = 500) String avatar,
        Gender gender,
        @Past(message = "生日必须早于今天") LocalDate birthday) {
}
