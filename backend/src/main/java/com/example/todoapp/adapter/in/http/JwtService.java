package com.example.todoapp.adapter.in.http;

import com.example.todoapp.domain.model.AuthenticatedUser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtService {

    private final SecretKey key;
    private final Duration ttl;

    public JwtService(
            @Value("${app.security.jwt.secret}") String base64Secret,
            @Value("${app.security.jwt.ttl-minutes:60}") long ttlMinutes) {
        // Base64-encoded because HMAC keys are raw bytes, and properties files are text.
        // Base64 is the conventional, safe way to embed binary in text config.
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret)); // jjwt constructor enforces the minimum key length for the algorithm
        this.ttl = Duration.ofMinutes(ttlMinutes);
    }

    public String issue(AuthenticatedUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    public UUID verify(String token) {
        String subject = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
        return UUID.fromString(subject);
    }
}
