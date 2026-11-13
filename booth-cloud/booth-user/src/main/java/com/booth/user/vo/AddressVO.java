package com.booth.user.vo;

public record AddressVO(
        Long id,
        String receiverName,
        String receiverPhone,
        String province,
        String city,
        String region,
        String detailAddress,
        String postCode,
        Boolean isDefault) {
}
