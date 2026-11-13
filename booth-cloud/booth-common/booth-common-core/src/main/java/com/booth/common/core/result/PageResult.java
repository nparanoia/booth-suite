package com.booth.common.core.result;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public record PageResult<T>(long total, long pageNum, long pageSize, List<T> list) {

    public static <T> PageResult<T> empty(long pageNum, long pageSize) {
        return new PageResult<>(0, pageNum, pageSize, Collections.emptyList());
    }

    public <R> PageResult<R> map(Function<? super T, ? extends R> mapper) {
        List<R> mapped = list.stream().<R>map(mapper).toList();
        return new PageResult<>(total, pageNum, pageSize, mapped);
    }
}
