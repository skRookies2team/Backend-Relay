package com.story.relay.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * JWT Token Provider for relay-server
 * Validates JWT tokens issued by story-backend
 * Uses the same secret key as story-backend for token validation
 */
@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKeyString;

    @Value("${jwt.access-token-validity}")
    private long accessTokenValidity;

    private SecretKey secretKey;

    /**
     * Initialize secret key after properties are loaded
     */
    @PostConstruct
    protected void init() {
        this.secretKey = Keys.hmacShaKeyFor(secretKeyString.getBytes(StandardCharsets.UTF_8));
        log.info("JWT TokenProvider initialized with HS256 algorithm");
    }

    /**
     * Validate JWT token
     * @param token JWT token string
     * @return true if token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            log.debug("JWT token validation successful");
            return true;
        } catch (JwtException e) {
            log.warn("JWT token validation failed: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            log.warn("JWT token is empty or malformed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extract username (subject) from JWT token
     * @param token JWT token string
     * @return username extracted from token
     */
    public String getUsername(String token) {
        try {
            String username = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
            log.debug("Extracted username from token: {}", username);
            return username;
        } catch (JwtException e) {
            log.error("Failed to extract username from token: {}", e.getMessage());
            throw new RuntimeException("Invalid JWT token", e);
        }
    }

    /**
     * Check if token is expired
     * This is handled automatically by validateToken(), but provided for explicit checks
     * @param token JWT token string
     * @return true if token is expired
     */
    public boolean isTokenExpired(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return false;
        } catch (JwtException e) {
            log.debug("Token expired or invalid: {}", e.getMessage());
            return true;
        }
    }
}
