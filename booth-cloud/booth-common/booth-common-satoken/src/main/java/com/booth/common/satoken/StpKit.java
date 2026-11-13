package com.booth.common.satoken;

import cn.dev33.satoken.stp.StpLogic;

/**
 * 会员和管理员是两套独立的登录体系：Token 存在 Redis 的不同 key 下，
 * 会员 Token 不能访问后台，反之亦然。
 */
public final class StpKit {

    public static final StpLogic MEMBER = new StpLogic("member");
    public static final StpLogic ADMIN = new StpLogic("admin");

    private static final String BEARER = "Bearer ";

    /**
     * 从 Authorization 请求头取出 Token，格式不对返回 null。
     */
    public static String bearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER)) {
            return null;
        }
        String token = authorization.substring(BEARER.length()).trim();
        return token.isEmpty() ? null : token;
    }

    private StpKit() {
    }
}
