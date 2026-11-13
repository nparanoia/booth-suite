package com.booth.trade.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SeckillOrderRequest(
        @NotBlank String path,
        @NotNull @Min(1) Integer quantity,
        @NotNull Long addressId) {
}
