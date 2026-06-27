package com.personaflow.commerce.user.service;

import com.personaflow.commerce.common.error.BusinessException;
import com.personaflow.commerce.common.error.ErrorCode;
import com.personaflow.commerce.user.api.CurrentUserProvider;
import com.personaflow.commerce.user.api.model.AddressSnapshot;
import com.personaflow.commerce.user.api.model.CurrentUser;
import com.personaflow.commerce.user.dto.CreateAddressRequest;
import com.personaflow.commerce.user.dto.UpdateAddressRequest;
import com.personaflow.commerce.user.entity.AddressEntity;
import com.personaflow.commerce.user.mapper.AddressMapper;
import com.personaflow.commerce.user.vo.AddressVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private AddressMapper addressMapper;

    @InjectMocks
    private AddressService addressService;

    @Test
    void listCurrentUserAddressesReturnsOwnedAddresses() {
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser());
        when(addressMapper.selectList(any())).thenReturn(List.of(
                address(10L, 10001L, "Alice", true),
                address(11L, 10001L, "Bob", false)
        ));

        List<AddressVO> result = addressService.listCurrentUserAddresses();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).addressId()).isEqualTo(10L);
        assertThat(result.get(0).isDefault()).isTrue();
        assertThat(result.get(1).recipientName()).isEqualTo("Bob");
    }

    @Test
    void createAddressInsertsAddressForCurrentUser() {
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser());
        when(addressMapper.selectCount(any())).thenReturn(1L);
        when(addressMapper.insert(any(AddressEntity.class))).thenAnswer(invocation -> {
            AddressEntity address = invocation.getArgument(0);
            address.setId(20L);
            return 1;
        });

        AddressVO result = addressService.createAddress(createRequest(false));

        assertThat(result.addressId()).isEqualTo(20L);
        assertThat(result.recipientPhone()).isEqualTo("13800000000");
        assertThat(result.isDefault()).isFalse();

        ArgumentCaptor<AddressEntity> addressCaptor = ArgumentCaptor.forClass(AddressEntity.class);
        verify(addressMapper).insert(addressCaptor.capture());
        AddressEntity inserted = addressCaptor.getValue();
        assertThat(inserted.getUserId()).isEqualTo(10001L);
        assertThat(inserted.getRecipientPhone()).isEqualTo("13800000000");
        assertThat(inserted.getDefaultAddress()).isFalse();
    }

    @Test
    void createDefaultAddressClearsExistingDefaultBeforeInsert() {
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser());
        when(addressMapper.update(isNull(), any())).thenReturn(1);
        when(addressMapper.insert(any(AddressEntity.class))).thenAnswer(invocation -> {
            AddressEntity address = invocation.getArgument(0);
            address.setId(21L);
            return 1;
        });

        addressService.createAddress(createRequest(true));

        InOrder inOrder = inOrder(addressMapper);
        inOrder.verify(addressMapper).update(isNull(), any());
        inOrder.verify(addressMapper).insert(any(AddressEntity.class));

        ArgumentCaptor<AddressEntity> addressCaptor = ArgumentCaptor.forClass(AddressEntity.class);
        verify(addressMapper).insert(addressCaptor.capture());
        assertThat(addressCaptor.getValue().getDefaultAddress()).isTrue();
    }

    @Test
    void updateOwnedAddressSucceeds() {
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser());
        when(addressMapper.selectOne(any())).thenReturn(
                address(10L, 10001L, "Old Name", false),
                address(10L, 10001L, "New Name", true)
        );
        when(addressMapper.update(isNull(), any())).thenReturn(1);

        AddressVO result = addressService.updateAddress(10L, updateRequest(true));

        assertThat(result.addressId()).isEqualTo(10L);
        assertThat(result.recipientName()).isEqualTo("New Name");
        assertThat(result.isDefault()).isTrue();
        verify(addressMapper, times(2)).update(isNull(), any());
    }

    @Test
    void updateOtherUsersAddressReturnsNotFound() {
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser());
        when(addressMapper.selectOne(any())).thenReturn(null);

        assertBusinessError(
                () -> addressService.updateAddress(99L, updateRequest(false)),
                ErrorCode.ACCOUNT_ADDRESS_NOT_FOUND
        );
        verify(addressMapper, never()).update(isNull(), any());
    }

    @Test
    void deleteOwnedAddressSucceeds() {
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser());
        when(addressMapper.selectOne(any())).thenReturn(address(10L, 10001L, "Alice", true));
        when(addressMapper.delete(any())).thenReturn(1);

        addressService.deleteAddress(10L);

        verify(addressMapper).delete(any());
    }

    @Test
    void setDefaultAddressClearsExistingDefaultAndSetsTarget() {
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser());
        when(addressMapper.selectOne(any())).thenReturn(
                address(10L, 10001L, "Alice", false),
                address(10L, 10001L, "Alice", true)
        );
        when(addressMapper.update(isNull(), any())).thenReturn(1);

        AddressVO result = addressService.setDefaultAddress(10L);

        assertThat(result.isDefault()).isTrue();
        verify(addressMapper, times(2)).update(isNull(), any());
    }

    @Test
    void requireOwnedAddressReturnsSnapshot() {
        when(addressMapper.selectOne(any())).thenReturn(address(10L, 10001L, "Alice", true));

        AddressSnapshot snapshot = addressService.requireOwnedAddress(10001L, 10L);

        assertThat(snapshot.addressId()).isEqualTo(10L);
        assertThat(snapshot.recipientName()).isEqualTo("Alice");
        assertThat(snapshot.recipientPhone()).isEqualTo("13800000000");
        assertThat(snapshot.province()).isEqualTo("Zhejiang");
    }

    @Test
    void requireOwnedAddressThrowsWhenAddressDoesNotBelongToUser() {
        when(addressMapper.selectOne(any())).thenReturn(null);

        assertBusinessError(
                () -> addressService.requireOwnedAddress(10001L, 99L),
                ErrorCode.ACCOUNT_ADDRESS_NOT_FOUND
        );
    }

    private CurrentUser currentUser() {
        return new CurrentUser(10001L, Set.of("ROLE_USER"));
    }

    private CreateAddressRequest createRequest(boolean defaultAddress) {
        return new CreateAddressRequest(
                "Alice",
                "13800000000",
                "Zhejiang",
                "Hangzhou",
                "Xihu",
                "No. 1 Road",
                "310000",
                defaultAddress
        );
    }

    private UpdateAddressRequest updateRequest(boolean defaultAddress) {
        return new UpdateAddressRequest(
                "New Name",
                "13900000000",
                "Zhejiang",
                "Hangzhou",
                "Xihu",
                "No. 2 Road",
                "310001",
                defaultAddress
        );
    }

    private AddressEntity address(Long id, Long userId, String recipientName, boolean defaultAddress) {
        AddressEntity address = new AddressEntity();
        address.setId(id);
        address.setUserId(userId);
        address.setRecipientName(recipientName);
        address.setRecipientPhone("13800000000");
        address.setProvince("Zhejiang");
        address.setCity("Hangzhou");
        address.setDistrict("Xihu");
        address.setDetailAddress("No. 1 Road");
        address.setPostalCode("310000");
        address.setDefaultAddress(defaultAddress);
        return address;
    }

    private void assertBusinessError(Runnable operation, ErrorCode expectedErrorCode) {
        assertThatThrownBy(operation::run)
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(expectedErrorCode);
    }
}
