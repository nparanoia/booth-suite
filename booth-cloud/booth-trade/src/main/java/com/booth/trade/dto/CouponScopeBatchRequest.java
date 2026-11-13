package com.booth.trade.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CouponScopeBatchRequest(@NotNull @Size(max = 100) @Valid List<CouponScopeRequest> scopes) {
}
