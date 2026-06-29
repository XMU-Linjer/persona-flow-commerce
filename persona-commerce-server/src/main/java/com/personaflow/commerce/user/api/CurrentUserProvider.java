package com.personaflow.commerce.user.api;

import com.personaflow.commerce.user.api.model.CurrentUser;

import java.util.Optional;

public interface CurrentUserProvider {

    CurrentUser requireCurrentUser();

    Optional<CurrentUser> findCurrentUser();
}
