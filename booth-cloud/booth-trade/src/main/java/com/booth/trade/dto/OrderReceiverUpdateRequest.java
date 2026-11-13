package com.booth.trade.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 待发货订单修改收货信息。 */
public record OrderReceiverUpdateRequest(
        @NotBlank @Size(max = 64) String receiverName,
        @NotBlank @Size(max = 32) String receiverPhone,
        @NotBlank @Size(max = 32) String receiverProvince,
        @NotBlank @Size(max = 32) String receiverCity,
        @NotBlank @Size(max = 32) String receiverRegion,
        @NotBlank @Size(max = 200) String receiverDetailAddress
) {
}
