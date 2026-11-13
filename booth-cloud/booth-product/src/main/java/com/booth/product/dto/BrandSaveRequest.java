package com.booth.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BrandSaveRequest(
        @NotBlank(message = "品牌名称不能为空") @Size(max = 64) String name,
        @Pattern(regexp = "[A-Z]", message = "首字母必须是一个大写字母") String firstLetter,
        @Size(max = 500) String logo,
        String story,
        Integer sort,
        Boolean showStatus) {
}
