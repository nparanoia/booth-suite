package com.booth.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param parentId 只在新增时生效，不传表示一级分类；修改时忽略，分类不支持移动
 */
public record CategorySaveRequest(
        Long parentId,
        @NotBlank(message = "分类名称不能为空") @Size(max = 64) String name,
        @Size(max = 500) String icon,
        @Size(max = 255) String keywords,
        Integer sort,
        Boolean navStatus,
        Boolean showStatus) {
}
