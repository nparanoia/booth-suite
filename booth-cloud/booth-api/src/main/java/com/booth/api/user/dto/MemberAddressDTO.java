package com.booth.api.user.dto;

public record MemberAddressDTO(
        Long id,
        String receiverName,
        String receiverPhone,
        String province,
        String city,
        String region,
        String detailAddress,
        String postCode) {
}
