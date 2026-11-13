package com.booth.user.vo;

import java.util.ArrayList;
import java.util.List;

public record PermissionTreeVO(
        Long id,
        String code,
        String name,
        Integer type,
        List<PermissionTreeVO> children) {

    public PermissionTreeVO {
        children = children == null ? new ArrayList<>() : children;
    }
}
