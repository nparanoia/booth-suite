package com.booth.api.user.dto;

public record MemberBriefDTO(
        Long id,
        String username,
        String nickname,
        boolean enabled,
        int integration) {
}
