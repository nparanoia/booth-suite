package com.booth.trade.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record MockPayNotifyRequest(
        @NotBlank String orderSn,
        @NotNull BigDecimal amount,
        @NotBlank String tradeStatus,
        @NotBlank String channelTradeNo,
        @NotNull Long timestamp,
        @NotBlank String sign) {
}
