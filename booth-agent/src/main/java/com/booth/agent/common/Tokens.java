package com.booth.agent.common;

import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

public final class Tokens {

    private Tokens() {
    }

    public static String bearer(String header) {
        if (!StringUtils.hasText(header)) {
            throw new ApiException(401, 40100, "请先登录，并在 Authorization 头携带 Bearer Token");
        }
        String token = header.startsWith("Bearer ") ? header.substring(7).trim() : header.trim();
        if (!StringUtils.hasText(token)) {
            throw new ApiException(401, 40100, "请先登录，并在 Authorization 头携带 Bearer Token");
        }
        return token;
    }

    /** 日志里只留指纹，不留会员令牌原文。 */
    public static String fingerprint(String token) {
        if (!StringUtils.hasText(token)) {
            return "-";
        }
        return DigestUtils.md5DigestAsHex(token.getBytes(StandardCharsets.UTF_8));
    }
}
