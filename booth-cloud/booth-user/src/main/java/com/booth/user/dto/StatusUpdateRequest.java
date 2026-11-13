package com.booth.user.dto;

import com.booth.user.enums.AccountStatus;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(@NotNull(message = "状态不能为空") AccountStatus status) {
}
