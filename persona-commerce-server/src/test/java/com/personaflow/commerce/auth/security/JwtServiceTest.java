package com.personaflow.commerce.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    @Test
    void generatedTokenContainsOnlyExpectedAccountClaims() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-jwt-secret-with-at-least-thirty-two-characters");
        properties.setExpiresIn(7200L);
        JwtService jwtService = new JwtService(properties);

        String token = jwtService.generateAccessToken(10003L, Set.of("ROLE_USER", "ROLE_ADMIN"));

        Claims claims = Jwts.parser()
                .verifyWith(signingKey(properties))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.getSubject()).isEqualTo("10003");
        assertThat(claims.getId()).isNotBlank();
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isNotNull();
        assertThat(claims.getExpiration().toInstant())
                .isAfter(claims.getIssuedAt().toInstant());
        assertThat(claims.get("roles", List.class))
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
        assertThat(claims).doesNotContainKeys("passwordHash", "displayName", "address", "phone");
    }

    private SecretKey signingKey(JwtProperties properties) {
        return Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
