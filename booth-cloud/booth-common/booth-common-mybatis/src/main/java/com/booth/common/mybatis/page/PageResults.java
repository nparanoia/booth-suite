package com.booth.common.mybatis.page;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.booth.common.core.result.PageResult;

import java.util.function.Function;

public final class PageResults {

    private PageResults() {
    }

    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    public static <E, T> PageResult<T> of(IPage<E> page, Function<? super E, ? extends T> mapper) {
        return of(page).map(mapper);
    }
}
