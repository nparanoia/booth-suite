package com.booth.agent.common;

public record Result<T>(int code, String message, T data, boolean success) {

    public static <T> Result<T> ok(T data) {
        return new Result<>(0, "ok", data, true);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null, false);
    }
}
