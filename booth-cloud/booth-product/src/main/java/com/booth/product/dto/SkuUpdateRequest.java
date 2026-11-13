package com.booth.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 库存走单独的接口，因为它要和锁定库存做条件比较。
 */
public record SkuUpdateRequest(
        @DecimalMin(value = "0.01", message = "价格至少 0.01") BigDecimal price,
        @Size(max = 500) String pic,
        @Min(0) Integer lowStock) {
}
