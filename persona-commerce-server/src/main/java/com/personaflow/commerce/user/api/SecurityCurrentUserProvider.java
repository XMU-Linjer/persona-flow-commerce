package com.personaflow.commerce.user.api;

import com.personaflow.commerce.auth.security.AuthenticatedUser;
import com.personaflow.commerce.common.error.BusinessException;
import com.personaflow.commerce.common.error.ErrorCode;
import com.personaflow.commerce.user.api.model.CurrentUser;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SecurityCurrentUserProvider implements CurrentUserProvider {

    @Override
    public CurrentUser requireCurrentUser() {
        return findCurrentUser()
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_UNAUTHORIZED));
    }

    @Override
    public Optional<CurrentUser> findCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || !(authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser)) {
            return Optional.empty();
        }

        return Optional.of(new CurrentUser(authenticatedUser.userId(), authenticatedUser.roles()));
    }
}
