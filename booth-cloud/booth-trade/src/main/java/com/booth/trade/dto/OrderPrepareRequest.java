package com.booth.trade.dto;

import com.booth.trade.enums.OrderSource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record OrderPrepareRequest(
        @NotNull OrderSource source,
        @Size(max = 50) @Valid List<OrderSkuRequest> items,
        Long memberCouponId,
        @Min(0) Integer useIntegration) {
}
