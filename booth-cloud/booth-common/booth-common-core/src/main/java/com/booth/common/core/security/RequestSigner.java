package com.booth.common.core.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * 网关与服务、服务与服务之间共享同一个密钥，用 HMAC-SHA256 对身份头签名。
 * 签名内容带时间戳，超过 {@link #MAX_SKEW_SECONDS} 的请求视为过期，缩小重放窗口。
 */
public final class RequestSigner {

    public static final long MAX_SKEW_SECONDS = 300;
    private static final String ALGORITHM = "HmacSHA256";
    private static final int MIN_SECRET_LENGTH = 32;

    private final SecretKeySpec key;

    public RequestSigner(String secret) {
        if (secret == null || secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException("booth.security.secret 至少 " + MIN_SECRET_LENGTH + " 个字符");
        }
        this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM);
    }

    public String signIdentity(String memberId, String adminId, String perms, long epochSeconds) {
        return hmac("identity|" + nullToEmpty(memberId) + "|" + nullToEmpty(adminId) + "|"
                + nullToEmpty(perms) + "|" + epochSeconds);
    }

    public String signInner(long epochSeconds) {
        return hmac("inner|" + epochSeconds);
    }

    /**
     * 对调用方拼好的原文签名。模拟支付回调用它，和生产环境的验签走同一套算法。
     */
    public String signContent(String content) {
        return hmac(content);
    }

    /**
     * 常量时间比较，避免通过响应耗时逐字节猜出签名。
     */
    public static boolean matches(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }

    public static boolean withinSkew(long epochSeconds, long nowEpochSeconds) {
        return Math.abs(nowEpochSeconds - epochSeconds) <= MAX_SKEW_SECONDS;
    }

    private String hmac(String content) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
