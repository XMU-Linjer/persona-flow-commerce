package com.personaflow.commerce.user.api;

import com.personaflow.commerce.user.api.model.AddressSnapshot;

public interface AddressQueryApi {

    AddressSnapshot requireOwnedAddress(Long userId, Long addressId);
}
