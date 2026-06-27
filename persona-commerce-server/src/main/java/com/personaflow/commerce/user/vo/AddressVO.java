package com.personaflow.commerce.user.vo;

public record AddressVO(
        Long addressId,
        String recipientName,
        String recipientPhone,
        String province,
        String city,
        String district,
        String detailAddress,
        String postalCode,
        Boolean isDefault
) {
}
