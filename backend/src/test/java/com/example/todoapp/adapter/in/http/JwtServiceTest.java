package com.example.todoapp.adapter.in.http;

import com.example.todoapp.domain.model.AuthenticatedUser;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    // 32 random bytes, Base64-encoded — meets HS256 minimum (256 bits).
    private static final String SECRET_B64 =
            "dGVzdC1zZWNyZXQtMzItYnl0ZXMtbG9uZy1mb3ItaG1hYy1zaGEyNTYh";
    private static final long TTL_MINUTES = 60;

    private final JwtService jwtService = new JwtService(SECRET_B64, TTL_MINUTES);

    @Test
    void issue_returnsNonEmptyToken() {
        AuthenticatedUser user = new AuthenticatedUser(UUID.randomUUID(), "alice");

        String token = jwtService.issue(user);

        assertNotNull(token);
        assertTrue(token.length() > 0);
    }

    @Test
    void verify_returnsUserIdFromIssuedToken() {
        UUID userId = UUID.randomUUID();
        AuthenticatedUser user = new AuthenticatedUser(userId, "alice");

        String token = jwtService.issue(user);

        assertEquals(userId, jwtService.verify(token));
    }

    @Test
    void verify_malformedToken_throws() {
        assertThrows(RuntimeException.class, () -> jwtService.verify("not-a-real-token"));
    }

    @Test
    void verify_emptyToken_throws() {
        assertThrows(RuntimeException.class, () -> jwtService.verify(""));
    }

    @Test
    void issuedToken_hasExpirationApproximatelyNowPlusTtl() {
        Instant before = Instant.now();
        String token = jwtService.issue(new AuthenticatedUser(UUID.randomUUID(), "alice"));
        Instant after = Instant.now();

        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET_B64));
        Date exp = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration();

        assertNotNull(exp, "token must carry an exp claim");
        // JWT exp is serialized to whole seconds, so widen the window by 1s on each side.
        Instant earliestExpected = before.plusSeconds(TTL_MINUTES * 60 - 1);
        Instant latestExpected = after.plusSeconds(TTL_MINUTES * 60 + 1);
        assertTrue(
                !exp.toInstant().isBefore(earliestExpected) && !exp.toInstant().isAfter(latestExpected),
                () -> "exp " + exp.toInstant() + " not within [" + earliestExpected + ", " + latestExpected + "]");
    }

    @Test
    void verify_expiredToken_throws() {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET_B64));
        String expiredToken = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .issuedAt(Date.from(Instant.now().minusSeconds(3600)))
                .expiration(Date.from(Instant.now().minusSeconds(60)))
                .signWith(key)
                .compact();

        assertThrows(ExpiredJwtException.class, () -> jwtService.verify(expiredToken));
    }
}
