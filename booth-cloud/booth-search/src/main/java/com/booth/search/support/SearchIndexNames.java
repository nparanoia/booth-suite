package com.booth.search.support;

public final class SearchIndexNames {

    public static final String ALIAS = "product";
    public static final String REBUILD_LOCK = "booth:search:rebuild:lock";
    public static final String REBUILD_INDEX = "booth:search:rebuild:index";

    private SearchIndexNames() {
    }

    public static String newIndex() {
        return "product_v" + System.currentTimeMillis();
    }
}
