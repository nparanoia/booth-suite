package com.booth.api.product.dto;

import jakarta.validation.constraints.NotBlank;

public record OrderSnRequest(@NotBlank String orderSn) {
}
