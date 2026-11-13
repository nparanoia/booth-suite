package com.booth.product.vo;

import java.util.ArrayList;
import java.util.List;

public record CategoryTreeVO(
        Long id,
        Long parentId,
        String name,
        Integer level,
        String icon,
        String keywords,
        Integer sort,
        Boolean navStatus,
        Boolean showStatus,
        List<CategoryTreeVO> children) {

    public CategoryTreeVO {
        children = children == null ? new ArrayList<>() : children;
    }
}
