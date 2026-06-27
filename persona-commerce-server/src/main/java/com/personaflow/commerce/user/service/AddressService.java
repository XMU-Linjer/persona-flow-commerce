package com.personaflow.commerce.user.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.personaflow.commerce.common.error.BusinessException;
import com.personaflow.commerce.common.error.ErrorCode;
import com.personaflow.commerce.user.api.AddressQueryApi;
import com.personaflow.commerce.user.api.CurrentUserProvider;
import com.personaflow.commerce.user.api.model.AddressSnapshot;
import com.personaflow.commerce.user.api.model.CurrentUser;
import com.personaflow.commerce.user.dto.CreateAddressRequest;
import com.personaflow.commerce.user.dto.UpdateAddressRequest;
import com.personaflow.commerce.user.entity.AddressEntity;
import com.personaflow.commerce.user.mapper.AddressMapper;
import com.personaflow.commerce.user.vo.AddressVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AddressService implements AddressQueryApi {

    private final CurrentUserProvider currentUserProvider;
    private final AddressMapper addressMapper;

    public AddressService(
            CurrentUserProvider currentUserProvider,
            AddressMapper addressMapper
    ) {
        this.currentUserProvider = currentUserProvider;
        this.addressMapper = addressMapper;
    }

    public List<AddressVO> listCurrentUserAddresses() {
        CurrentUser currentUser = currentUserProvider.requireCurrentUser();
        return addressMapper.selectList(
                        Wrappers.<AddressEntity>query()
                                .eq("user_id", currentUser.userId())
                                .orderByDesc("is_default")
                                .orderByDesc("updated_at")
                                .orderByDesc("id")
                )
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Transactional
    public AddressVO createAddress(CreateAddressRequest request) {
        CurrentUser currentUser = currentUserProvider.requireCurrentUser();
        boolean defaultAddress = shouldCreateDefaultAddress(currentUser.userId(), request.isDefault());
        if (defaultAddress) {
            clearDefaultAddress(currentUser.userId());
        }

        AddressEntity address = new AddressEntity();
        address.setUserId(currentUser.userId());
        fillAddress(address, request);
        address.setDefaultAddress(defaultAddress);
        expectOneRow(addressMapper.insert(address), "Failed to create address");
        return toVO(address);
    }

    @Transactional
    public AddressVO updateAddress(Long addressId, UpdateAddressRequest request) {
        CurrentUser currentUser = currentUserProvider.requireCurrentUser();
        requireOwnedAddressEntity(currentUser.userId(), addressId);

        boolean defaultAddress = Boolean.TRUE.equals(request.isDefault());
        if (defaultAddress) {
            clearDefaultAddress(currentUser.userId());
        }

        int affectedRows = addressMapper.update(
                null,
                Wrappers.<AddressEntity>update()
                        .eq("id", addressId)
                        .eq("user_id", currentUser.userId())
                        .set("recipient_name", request.recipientName().trim())
                        .set("recipient_phone", request.recipientPhone().trim())
                        .set("province", request.province().trim())
                        .set("city", request.city().trim())
                        .set("district", request.district().trim())
                        .set("detail_address", request.detailAddress().trim())
                        .set("postal_code", normalizeOptionalText(request.postalCode()))
                        .set("is_default", defaultAddress)
        );
        expectOneRow(affectedRows, "Failed to update address");

        return toVO(requireOwnedAddressEntity(currentUser.userId(), addressId));
    }

    @Transactional
    public void deleteAddress(Long addressId) {
        CurrentUser currentUser = currentUserProvider.requireCurrentUser();
        requireOwnedAddressEntity(currentUser.userId(), addressId);

        int affectedRows = addressMapper.delete(
                Wrappers.<AddressEntity>query()
                        .eq("id", addressId)
                        .eq("user_id", currentUser.userId())
        );
        expectOneRow(affectedRows, "Failed to delete address");
    }

    @Transactional
    public AddressVO setDefaultAddress(Long addressId) {
        CurrentUser currentUser = currentUserProvider.requireCurrentUser();
        requireOwnedAddressEntity(currentUser.userId(), addressId);
        clearDefaultAddress(currentUser.userId());
        expectOneRow(
                addressMapper.update(
                        null,
                        Wrappers.<AddressEntity>update()
                                .eq("id", addressId)
                                .eq("user_id", currentUser.userId())
                                .set("is_default", true)
                ),
                "Failed to set default address"
        );
        return toVO(requireOwnedAddressEntity(currentUser.userId(), addressId));
    }

    @Override
    public AddressSnapshot requireOwnedAddress(Long userId, Long addressId) {
        AddressEntity address = requireOwnedAddressEntity(userId, addressId);
        return new AddressSnapshot(
                address.getId(),
                address.getRecipientName(),
                address.getRecipientPhone(),
                address.getProvince(),
                address.getCity(),
                address.getDistrict(),
                address.getDetailAddress(),
                address.getPostalCode()
        );
    }

    private boolean shouldCreateDefaultAddress(Long userId, Boolean requestedDefault) {
        if (Boolean.TRUE.equals(requestedDefault)) {
            return true;
        }
        Long addressCount = addressMapper.selectCount(
                Wrappers.<AddressEntity>query()
                        .eq("user_id", userId)
        );
        return addressCount == null || addressCount == 0;
    }

    private void clearDefaultAddress(Long userId) {
        addressMapper.update(
                null,
                Wrappers.<AddressEntity>update()
                        .eq("user_id", userId)
                        .eq("is_default", true)
                        .set("is_default", false)
        );
    }

    private AddressEntity requireOwnedAddressEntity(Long userId, Long addressId) {
        AddressEntity address = addressMapper.selectOne(
                Wrappers.<AddressEntity>query()
                        .eq("id", addressId)
                        .eq("user_id", userId)
        );
        if (address == null) {
            throw new BusinessException(ErrorCode.ACCOUNT_ADDRESS_NOT_FOUND);
        }
        return address;
    }

    private void fillAddress(AddressEntity address, CreateAddressRequest request) {
        address.setRecipientName(request.recipientName().trim());
        address.setRecipientPhone(request.recipientPhone().trim());
        address.setProvince(request.province().trim());
        address.setCity(request.city().trim());
        address.setDistrict(request.district().trim());
        address.setDetailAddress(request.detailAddress().trim());
        address.setPostalCode(normalizeOptionalText(request.postalCode()));
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private AddressVO toVO(AddressEntity address) {
        return new AddressVO(
                address.getId(),
                address.getRecipientName(),
                address.getRecipientPhone(),
                address.getProvince(),
                address.getCity(),
                address.getDistrict(),
                address.getDetailAddress(),
                address.getPostalCode(),
                address.getDefaultAddress()
        );
    }

    private void expectOneRow(int affectedRows, String message) {
        if (affectedRows != 1) {
            throw new IllegalStateException(message);
        }
    }
}
