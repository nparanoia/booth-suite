package com.booth.trade.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

public record SeckillSessionRequest(
        @NotBlank @Size(max = 64) String name,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        Boolean status) {
}
