package com.booth.product.dto;

import com.booth.product.enums.PublishStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PublishRequest(
        @NotEmpty(message = "商品 ID 不能为空") @Size(max = 100) List<Long> ids,
        @NotNull(message = "上下架状态不能为空") PublishStatus status) {
}
