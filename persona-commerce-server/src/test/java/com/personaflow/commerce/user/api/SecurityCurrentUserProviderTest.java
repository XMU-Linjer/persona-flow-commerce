package com.personaflow.commerce.user.api;

import com.personaflow.commerce.auth.security.AuthenticatedUser;
import com.personaflow.commerce.common.error.BusinessException;
import com.personaflow.commerce.common.error.ErrorCode;
import com.personaflow.commerce.user.api.model.CurrentUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityCurrentUserProviderTest {

    private final SecurityCurrentUserProvider provider = new SecurityCurrentUserProvider();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void requireCurrentUserReturnsAuthenticatedUserFromSecurityContext() {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                10001L,
                Set.of("ROLE_USER")
        );
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                authenticatedUser,
                null,
                authenticatedUser.roles().stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        CurrentUser currentUser = provider.requireCurrentUser();

        assertThat(currentUser.userId()).isEqualTo(10001L);
        assertThat(currentUser.roles()).containsExactly("ROLE_USER");
        assertThat(provider.findCurrentUser()).contains(currentUser);
    }

    @Test
    void requireCurrentUserThrowsWhenNoAuthenticationExists() {
        assertThatThrownBy(provider::requireCurrentUser)
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(ErrorCode.ACCOUNT_UNAUTHORIZED);
    }

    @Test
    void findCurrentUserReturnsEmptyWhenNoAuthenticationExists() {
        assertThat(provider.findCurrentUser()).isEmpty();
    }
}
