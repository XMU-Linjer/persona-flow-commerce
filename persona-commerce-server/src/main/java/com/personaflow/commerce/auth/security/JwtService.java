package com.personaflow.commerce.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Service
public class JwtService {

    private final JwtProperties properties;
    private final Clock clock;

    public JwtService(JwtProperties properties) {
        this(properties, Clock.systemUTC());
    }

    JwtService(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public String generateAccessToken(Long userId, Set<String> roles) {
        Instant issuedAt = Instant.now(clock);
        Instant expiresAt = issuedAt.plusSeconds(properties.getExpiresIn());

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("roles", roles)
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey())
                .compact();
    }

    public long expiresIn() {
        return properties.getExpiresIn();
    }

    public AuthenticatedUser parseAuthenticatedUser(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return new AuthenticatedUser(
                Long.valueOf(claims.getSubject()),
                parseRoles(claims.get("roles"))
        );
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    private Set<String> parseRoles(Object rolesClaim) {
        if (!(rolesClaim instanceof Collection<?> roles)) {
            return Set.of();
        }

        Set<String> parsedRoles = new LinkedHashSet<>();
        for (Object role : roles) {
            parsedRoles.add(String.valueOf(role));
        }
        return parsedRoles;
    }
}
