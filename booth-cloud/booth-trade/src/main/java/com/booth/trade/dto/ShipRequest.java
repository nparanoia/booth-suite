package com.booth.trade.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ShipRequest(
        @NotBlank @Size(max = 64) String deliveryCompany,
        @NotBlank @Size(max = 64) String deliverySn) {
}
