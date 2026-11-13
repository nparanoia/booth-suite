package com.booth.api.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record StockLockRequest(
        @NotBlank String orderSn,
        @NotEmpty @Size(max = 50) @Valid List<StockItemDTO> items) {
}
