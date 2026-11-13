package com.booth.agent.trace;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class TraceMask {

    private static final Pattern PHONE = Pattern.compile("(?<!\\d)(1[3-9]\\d)\\d{4}(\\d{4})(?!\\d)");

    private TraceMask() {
    }

    public static String summarize(Object arguments) {
        if (!(arguments instanceof Map<?, ?> map)) {
            return maskText(arguments == null ? "" : arguments.toString());
        }
        Map<String, String> safe = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = String.valueOf(entry.getKey());
            if (sensitive(key)) {
                safe.put(key, "***");
            } else {
                safe.put(key, maskText(entry.getValue() == null ? "" : entry.getValue().toString()));
            }
        }
        return safe.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(","));
    }

    public static String maskText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return PHONE.matcher(text).replaceAll("$1****$2");
    }

    private static boolean sensitive(String key) {
        String normalized = key.toLowerCase(Locale.ROOT);
        return normalized.contains("token")
                || normalized.contains("authorization")
                || normalized.contains("apikey")
                || normalized.contains("api_key")
                || normalized.equals("phone")
                || normalized.equals("mobile");
    }
}
