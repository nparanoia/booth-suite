package com.booth.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ProductCreateRequest(
        @NotNull @Valid ProductSaveRequest product,
        @NotEmpty(message = "至少需要一个 SKU") @Size(max = 100) List<@Valid SkuCreateRequest> skus) {
}
