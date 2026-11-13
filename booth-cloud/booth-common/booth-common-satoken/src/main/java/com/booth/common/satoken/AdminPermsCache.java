package com.booth.common.satoken;

/**
 * 管理员权限码缓存：用户服务在登录和权限变更时写入，网关每次请求读取后放进身份头。
 * 值是逗号分隔的权限码字符串，不做对象序列化。
 */
public final class AdminPermsCache {

    public static final String WILDCARD = "*";

    private static final String KEY_PREFIX = "booth:admin:perms:";

    public static String key(Object adminId) {
        return KEY_PREFIX + adminId;
    }

    private AdminPermsCache() {
    }
}
