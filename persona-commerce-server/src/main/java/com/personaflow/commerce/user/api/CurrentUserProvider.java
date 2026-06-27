package com.personaflow.commerce.user.api;

import com.personaflow.commerce.user.api.model.CurrentUser;

public interface CurrentUserProvider {

    CurrentUser requireCurrentUser();
}
