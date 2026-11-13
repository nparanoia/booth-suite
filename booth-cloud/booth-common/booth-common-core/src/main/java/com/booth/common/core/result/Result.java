package com.booth.common.core.result;

import com.booth.common.core.exception.BizException;
import com.booth.common.core.exception.CommonErrorCode;
import com.booth.common.core.exception.ErrorCode;

public record Result<T>(int code, String message, T data) {

    public static <T> Result<T> ok() {
        return new Result<>(CommonErrorCode.SUCCESS.code(), CommonErrorCode.SUCCESS.message(), null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(CommonErrorCode.SUCCESS.code(), CommonErrorCode.SUCCESS.message(), data);
    }

    public static <T> Result<T> fail(ErrorCode errorCode) {
        return new Result<>(errorCode.code(), errorCode.message(), null);
    }

    public static <T> Result<T> fail(ErrorCode errorCode, String message) {
        return new Result<>(errorCode.code(), message, null);
    }

    public boolean isSuccess() {
        return code == CommonErrorCode.SUCCESS.code();
    }

    /**
     * 服务间调用拿到结果后使用：失败时把对方的错误码原样抛出，由调用方的全局异常处理返回给前端。
     */
    public T dataOrThrow() {
        if (!isSuccess()) {
            throw new BizException(ErrorCode.of(code, message));
        }
        return data;
    }
}
