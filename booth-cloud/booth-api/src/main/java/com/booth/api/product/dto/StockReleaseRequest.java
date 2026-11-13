package com.booth.api.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * items 在订单尚未锁定时用来写入“已释放”占位，挡住晚到的锁定请求。已有锁定记录时按记录释放，忽略 items。
 */
public record StockReleaseRequest(
        @NotBlank String orderSn,
        @Size(max = 50) @Valid List<StockItemDTO> items) {
}
