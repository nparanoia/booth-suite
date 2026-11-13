package com.booth.trade.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderSkuRequest(
        @NotNull Long skuId,
        @NotNull @Min(1) @Max(99) Integer quantity) {
}
