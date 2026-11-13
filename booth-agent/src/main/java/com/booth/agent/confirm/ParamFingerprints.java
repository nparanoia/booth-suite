package com.booth.agent.confirm;

import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** 写操作确认参数指纹，同人同参复用确认票、对账去重。 */
public final class ParamFingerprints {

    private ParamFingerprints() {
    }

    public static String of(String action, String orderSn, Long orderItemId, String reason, String description) {
        String raw = normalize(action) + '|'
                + normalize(orderSn) + '|'
                + (orderItemId == null ? "-" : orderItemId) + '|'
                + normalize(reason) + '|'
                + normalize(description);
        return DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }

    private static String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
