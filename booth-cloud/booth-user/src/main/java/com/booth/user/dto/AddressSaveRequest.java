package com.booth.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressSaveRequest(
        @NotBlank(message = "收货人不能为空") @Size(max = 64) String receiverName,
        @NotBlank @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确") String receiverPhone,
        @NotBlank(message = "省份不能为空") @Size(max = 32) String province,
        @NotBlank(message = "城市不能为空") @Size(max = 32) String city,
        @NotBlank(message = "区县不能为空") @Size(max = 32) String region,
        @NotBlank(message = "详细地址不能为空") @Size(max = 200) String detailAddress,
        @Size(max = 16) String postCode,
        Boolean isDefault) {
}
