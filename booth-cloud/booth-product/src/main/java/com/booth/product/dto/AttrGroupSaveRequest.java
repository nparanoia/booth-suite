package com.booth.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AttrGroupSaveRequest(
        @NotBlank(message = "分组名称不能为空") @Size(max = 64) String name) {
}
