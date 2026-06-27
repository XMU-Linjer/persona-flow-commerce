package com.personaflow.commerce.user.controller;

import com.personaflow.commerce.common.error.BusinessException;
import com.personaflow.commerce.common.error.ErrorCode;
import com.personaflow.commerce.common.error.GlobalExceptionHandler;
import com.personaflow.commerce.user.dto.CreateAddressRequest;
import com.personaflow.commerce.user.dto.UpdateAddressRequest;
import com.personaflow.commerce.user.service.AddressService;
import com.personaflow.commerce.user.vo.AddressVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AddressControllerTest {

    @Mock
    private AddressService addressService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AddressController(addressService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listAddressesReturnsSuccessResponse() throws Exception {
        when(addressService.listCurrentUserAddresses()).thenReturn(List.of(
                addressVO(10L, true),
                addressVO(11L, false)
        ));

        mockMvc.perform(get("/api/users/me/addresses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].addressId").value(10))
                .andExpect(jsonPath("$.data[0].isDefault").value(true))
                .andExpect(jsonPath("$.data[1].addressId").value(11));
    }

    @Test
    void createAddressReturnsSuccessResponse() throws Exception {
        when(addressService.createAddress(any())).thenReturn(addressVO(10L, true));

        mockMvc.perform(post("/api/users/me/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addressJson(true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.addressId").value(10))
                .andExpect(jsonPath("$.data.recipientPhone").value("13800000000"))
                .andExpect(jsonPath("$.data.isDefault").value(true));

        ArgumentCaptor<CreateAddressRequest> requestCaptor = ArgumentCaptor.forClass(CreateAddressRequest.class);
        verify(addressService).createAddress(requestCaptor.capture());
        assertThat(requestCaptor.getValue().recipientPhone()).isEqualTo("13800000000");
        assertThat(requestCaptor.getValue().isDefault()).isTrue();
    }

    @Test
    void invalidAddressReturnsValidationError() throws Exception {
        mockMvc.perform(post("/api/users/me/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recipientName": "",
                                  "recipientPhone": "13800000000",
                                  "province": "Zhejiang",
                                  "city": "Hangzhou",
                                  "district": "Xihu",
                                  "detailAddress": "No. 1 Road",
                                  "postalCode": "310000",
                                  "isDefault": false
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.errorCode").value("COMMON_VALIDATION_FAILED"));

        verifyNoInteractions(addressService);
    }

    @Test
    void updateAddressReturnsSuccessResponse() throws Exception {
        when(addressService.updateAddress(any(), any())).thenReturn(addressVO(10L, false));

        mockMvc.perform(put("/api/users/me/addresses/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addressJson(false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.addressId").value(10))
                .andExpect(jsonPath("$.data.isDefault").value(false));

        ArgumentCaptor<UpdateAddressRequest> requestCaptor = ArgumentCaptor.forClass(UpdateAddressRequest.class);
        verify(addressService).updateAddress(org.mockito.ArgumentMatchers.eq(10L), requestCaptor.capture());
        assertThat(requestCaptor.getValue().isDefault()).isFalse();
    }

    @Test
    void updateMissingAddressReturnsBusinessError() throws Exception {
        when(addressService.updateAddress(any(), any()))
                .thenThrow(new BusinessException(ErrorCode.ACCOUNT_ADDRESS_NOT_FOUND));

        mockMvc.perform(put("/api/users/me/addresses/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addressJson(false)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_ADDRESS_NOT_FOUND"));
    }

    @Test
    void deleteAddressReturnsSuccessResponse() throws Exception {
        mockMvc.perform(delete("/api/users/me/addresses/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(addressService).deleteAddress(10L);
    }

    @Test
    void deleteMissingAddressReturnsBusinessError() throws Exception {
        doThrow(new BusinessException(ErrorCode.ACCOUNT_ADDRESS_NOT_FOUND))
                .when(addressService)
                .deleteAddress(99L);

        mockMvc.perform(delete("/api/users/me/addresses/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_ADDRESS_NOT_FOUND"));
    }

    @Test
    void setDefaultAddressReturnsSuccessResponse() throws Exception {
        when(addressService.setDefaultAddress(10L)).thenReturn(addressVO(10L, true));

        mockMvc.perform(put("/api/users/me/addresses/10/default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.addressId").value(10))
                .andExpect(jsonPath("$.data.isDefault").value(true));
    }

    private AddressVO addressVO(Long addressId, boolean defaultAddress) {
        return new AddressVO(
                addressId,
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

    private String addressJson(boolean defaultAddress) {
        return """
                {
                  "recipientName": "Alice",
                  "recipientPhone": "13800000000",
                  "province": "Zhejiang",
                  "city": "Hangzhou",
                  "district": "Xihu",
                  "detailAddress": "No. 1 Road",
                  "postalCode": "310000",
                  "isDefault": %s
                }
                """.formatted(defaultAddress);
    }
}
