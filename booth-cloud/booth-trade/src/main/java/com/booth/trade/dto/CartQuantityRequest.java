package com.booth.trade.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CartQuantityRequest(
        @NotNull @Min(value = 1, message = "数量至少为 1") @Max(value = 99, message = "单件商品最多 99 件") Integer quantity) {
}
