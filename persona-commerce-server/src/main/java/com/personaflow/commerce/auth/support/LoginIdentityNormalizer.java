package com.personaflow.commerce.auth.support;

import java.util.Locale;

public final class LoginIdentityNormalizer {

    private LoginIdentityNormalizer() {
    }

    public static String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }
}
