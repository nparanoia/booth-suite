package com.booth.product.dto;

import com.booth.product.enums.AttrType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * @param groupId 只在新增时生效，属性不支持换分组
 */
public record AttrSaveRequest(
        Long groupId,
        @NotBlank(message = "属性名称不能为空") @Size(max = 64) String name,
        @NotNull(message = "属性类型不能为空") AttrType type,
        @Min(0) @Max(1) Integer inputType,
        @Size(max = 500) String inputList,
        @Min(0) @Max(2) Integer searchType,
        Integer sort) {
}
