package com.booth.trade.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReturnApplyRequest(
        @NotNull Long orderItemId,
        @NotBlank @Size(max = 64) String reason,
        @Size(max = 500) String description,
        @Size(max = 2000) String proofPics) {
}
