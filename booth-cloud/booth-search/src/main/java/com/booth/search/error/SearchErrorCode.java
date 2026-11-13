package com.booth.search.error;

import com.booth.common.core.exception.ErrorCode;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum SearchErrorCode implements ErrorCode {

    PAGE_TOO_DEEP(40001, "翻页过深"),
    REBUILD_BUSY(40002, "正在重建索引，请稍后再试");

    private final int code;
    private final String message;

    @Override
    public int code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
