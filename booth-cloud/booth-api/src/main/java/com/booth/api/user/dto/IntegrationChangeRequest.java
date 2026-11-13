package com.booth.api.user.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record IntegrationChangeRequest(
        @NotBlank @Size(max = 64) String bizNo,
        @NotNull @Min(1) Integer amount) {
}
