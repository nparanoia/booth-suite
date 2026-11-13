package com.booth.trade.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CouponSaveRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotNull @DecimalMin(value = "0") BigDecimal minPoint,
        @NotNull @Min(0) @Max(2) Integer useType,
        @NotNull @Min(1) Integer totalCount,
        @NotNull @Min(1) Integer perLimit,
        @NotNull LocalDateTime receiveStartTime,
        @NotNull LocalDateTime receiveEndTime,
        @NotNull LocalDateTime useStartTime,
        @NotNull LocalDateTime useEndTime,
        @Size(max = 200) String note) {
}
