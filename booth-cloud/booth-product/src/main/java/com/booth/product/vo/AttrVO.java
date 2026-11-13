package com.booth.product.vo;

import com.booth.product.enums.AttrType;

public record AttrVO(
        Long id,
        Long groupId,
        String name,
        AttrType type,
        Integer inputType,
        String inputList,
        Integer searchType,
        Integer sort) {
}
