package com.personaflow.commerce.user.controller;

import com.personaflow.commerce.common.api.ApiResponse;
import com.personaflow.commerce.user.dto.CreateAddressRequest;
import com.personaflow.commerce.user.dto.UpdateAddressRequest;
import com.personaflow.commerce.user.service.AddressService;
import com.personaflow.commerce.user.vo.AddressVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/users/me/addresses")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    public ApiResponse<List<AddressVO>> listAddresses() {
        return ApiResponse.success(addressService.listCurrentUserAddresses());
    }

    @PostMapping
    public ApiResponse<AddressVO> createAddress(@Valid @RequestBody CreateAddressRequest request) {
        return ApiResponse.success(addressService.createAddress(request));
    }

    @PutMapping("/{addressId}")
    public ApiResponse<AddressVO> updateAddress(
            @Positive @PathVariable Long addressId,
            @Valid @RequestBody UpdateAddressRequest request
    ) {
        return ApiResponse.success(addressService.updateAddress(addressId, request));
    }

    @DeleteMapping("/{addressId}")
    public ApiResponse<Void> deleteAddress(@Positive @PathVariable Long addressId) {
        addressService.deleteAddress(addressId);
        return ApiResponse.success(null);
    }

    @PutMapping("/{addressId}/default")
    public ApiResponse<AddressVO> setDefaultAddress(@Positive @PathVariable Long addressId) {
        return ApiResponse.success(addressService.setDefaultAddress(addressId));
    }
}
