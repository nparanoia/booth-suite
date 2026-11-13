package com.booth.api.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StockItemDTO(
        @NotNull Long skuId,
        @NotNull @Min(1) Integer quantity) {
}
