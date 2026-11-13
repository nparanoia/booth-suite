package com.booth.common.core.exception;

/**
 * 错误码约定：0 表示成功；4xx/5xx 与 HTTP 语义对齐的通用错误；
 * 各服务业务错误码按服务分段，用户 1xxxx、商品 2xxxx、交易 3xxxx、搜索 4xxxx。
 */
public interface ErrorCode {

    int code();

    String message();

    /**
     * 用于还原远程服务返回的错误码，调用方不需要依赖对方的错误码枚举。
     */
    static ErrorCode of(int code, String message) {
        return new SimpleErrorCode(code, message);
    }

    record SimpleErrorCode(int code, String message) implements ErrorCode {
    }
}
