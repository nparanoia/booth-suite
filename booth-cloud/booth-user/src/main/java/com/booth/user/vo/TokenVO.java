package com.booth.user.vo;

/**
 * 客户端后续请求带上请求头 Authorization: Bearer {token}。
 */
public record TokenVO(String token, long expiresIn) {
}
