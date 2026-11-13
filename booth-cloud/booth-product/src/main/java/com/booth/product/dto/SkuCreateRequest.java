package com.booth.product.dto;

import com.booth.api.product.dto.SkuSpec;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record SkuCreateRequest(
        @NotBlank(message = "SKU 编码不能为空") @Size(max = 64) String skuCode,
        @NotEmpty(message = "SKU 规格不能为空") List<SkuSpec> specs,
        @NotNull(message = "价格不能为空") @DecimalMin(value = "0.01", message = "价格至少 0.01") BigDecimal price,
        @Size(max = 500) String pic,
        @NotNull(message = "库存不能为空") @Min(0) Integer stock,
        @Min(0) Integer lowStock) {
}
