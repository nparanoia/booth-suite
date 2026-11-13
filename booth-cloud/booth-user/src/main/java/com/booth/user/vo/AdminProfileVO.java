package com.booth.user.vo;

import java.util.List;

/**
 * 后台前端据此决定显示哪些菜单和按钮；真正的权限校验仍在每个接口上做。
 */
public record AdminProfileVO(
        Long id,
        String username,
        String nickname,
        String avatar,
        List<String> roles,
        List<String> permissions) {
}
