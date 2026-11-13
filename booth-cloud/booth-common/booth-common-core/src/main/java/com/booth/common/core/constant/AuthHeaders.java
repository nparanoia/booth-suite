package com.booth.common.core.constant;

/**
 * 网关鉴权通过后写入下游请求的身份头。下游服务只信任这些头，不自己解析 Token；
 * 网关必须先删除客户端自带的同名头，否则客户端可以伪造身份。
 * 身份头连同时间戳由网关签名，下游验签，防止绕过网关直连服务端口时伪造身份。
 */
public final class AuthHeaders {

    public static final String MEMBER_ID = "X-Member-Id";
    public static final String ADMIN_ID = "X-Admin-Id";
    /** 管理员权限码，逗号分隔；超级管理员为 * */
    public static final String ADMIN_PERMS = "X-Admin-Perms";
    public static final String AUTH_TIME = "X-Auth-Time";
    public static final String AUTH_SIGN = "X-Auth-Sign";

    /** 服务间调用 /inner 接口时由 Feign 自动附加 */
    public static final String INNER_TIME = "X-Inner-Time";
    public static final String INNER_SIGN = "X-Inner-Sign";

    /** 网关转发前必须删除的全部头，客户端不能自己带 */
    public static final String[] GATEWAY_MANAGED = {
            MEMBER_ID, ADMIN_ID, ADMIN_PERMS, AUTH_TIME, AUTH_SIGN, INNER_TIME, INNER_SIGN
    };

    private AuthHeaders() {
    }
}
