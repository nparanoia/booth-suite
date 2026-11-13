package com.booth.user.service;

import com.booth.common.core.exception.BizException;
import com.booth.user.error.UserErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * 登录失败按 IP 限流，降低演示站撞库弱口令（如 admin123）的成功率。
 */
@Service
@RequiredArgsConstructor
public class LoginRateLimiter {

    private static final String PREFIX = "booth:login:fail:";
    private static final int MAX_FAILS = 10;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private final StringRedisTemplate redis;

    public void guard(String scope, String ip) {
        String key = key(scope, ip);
        String raw = redis.opsForValue().get(key);
        if (raw == null) {
            return;
        }
        try {
            if (Integer.parseInt(raw) >= MAX_FAILS) {
                throw new BizException(UserErrorCode.LOGIN_RATE_LIMITED);
            }
        } catch (NumberFormatException ignored) {
            redis.delete(key);
        }
    }

    public void recordFail(String scope, String ip) {
        String key = key(scope, ip);
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redis.expire(key, WINDOW);
        }
    }

    public void clear(String scope, String ip) {
        redis.delete(key(scope, ip));
    }

    private static String key(String scope, String ip) {
        String safeIp = StringUtils.hasText(ip) ? ip.trim() : "unknown";
        return PREFIX + scope + ":" + safeIp;
    }
}
