package com.booth.agent.common;

import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {

    private final int httpStatus;
    private final int code;

    public ApiException(int httpStatus, int code, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.code = code;
    }
}
