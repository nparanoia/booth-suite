package com.booth.trade.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record SeckillActivityRequest(
        @NotBlank @Size(max = 100) String title,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate) {
}
