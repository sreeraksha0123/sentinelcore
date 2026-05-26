package com.raksha.sentinelcore.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Stateless JWT utility — generates and validates HS256 tokens.
 *
 * <p>The secret is a Base64-encoded 256-bit key configured in {@code application.yml}.
 * In production, source this from a secrets manager (Vault, AWS Secrets Manager, etc.).
 */
@Service
@Slf4j
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms}")
    private Long expirationMs;

    // ── Token generation ─────────────────────────────────────

    public String generate(String email) {
        return Jwts.builder()
            .subject(email)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expirationMs))
            .signWith(key())
            .compact();
    }

    // ── Claims extraction ────────────────────────────────────

    public String extractEmail(String token) {
        return Jwts.parser()
            .verifyWith(key())
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .getSubject();
    }

    // ── Validation ───────────────────────────────────────────

    public boolean isValid(String token, UserDetails userDetails) {
        try {
            String email = extractEmail(token);
            return email.equals(userDetails.getUsername()) && !isExpired(token);
        } catch (Exception ex) {
            log.debug("JWT validation failed: {}", ex.getMessage());
            return false;
        }
    }

    // ── Private helpers ──────────────────────────────────────

    private SecretKey key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    private boolean isExpired(String token) {
        return Jwts.parser()
            .verifyWith(key())
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .getExpiration()
            .before(new Date());
    }
}
