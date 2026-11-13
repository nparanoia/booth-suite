package com.booth.trade.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * @param itemIds 不传表示全选或全不选
 */
public record CartCheckRequest(
        @Size(max = 100) List<Long> itemIds,
        @NotNull(message = "勾选状态不能为空") Boolean checked) {
}
