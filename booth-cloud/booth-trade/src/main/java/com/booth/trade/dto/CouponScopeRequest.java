package com.booth.trade.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CouponScopeRequest(
        @NotNull @Min(1) @Max(2) Integer scopeType,
        @NotNull Long targetId,
        @NotBlank @Size(max = 200) String targetName) {
}
