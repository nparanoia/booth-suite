package com.booth.trade.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SeckillSkuRequest(
        @NotNull Long activityId,
        @NotNull Long sessionId,
        @NotNull Long skuId,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 8, fraction = 2) BigDecimal seckillPrice,
        @NotNull @Min(1) Integer seckillStock,
        @NotNull @Min(1) Integer perLimit,
        Integer sort) {
}
