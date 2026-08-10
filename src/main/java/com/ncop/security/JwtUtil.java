package com.ncop.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

@Component
public class JwtUtil {

    private final SecretKey key = Keys.hmacShaKeyFor(
            "this-is-a-temporary-demo-secret-key-1234567890".getBytes());

    // Token expiry times
    private static final long ACCESS_TOKEN_EXPIRY_MS = 1000 * 60 * 30; // 30 minutes
    private static final long REFRESH_TOKEN_EXPIRY_MS = 1000 * 60 * 60 * 24 * 7; // 7 days

    /**
     * Generate access token (30 minutes expiry)
     */
    public String generateAccessToken(String email, List<String> roles) {
        return Jwts.builder()
                .setSubject(email)
                .claim("roles", roles)
                .claim("type", "access")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRY_MS))
                .signWith(key)
                .compact();
    }

    /**
     * Generate refresh token (7 days expiry)
     */
    public String generateRefreshToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .claim("type", "refresh")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRY_MS))
                .signWith(key)
                .compact();
    }

    /**
     * Legacy method for backward compatibility
     */
    public String generateToken(String email, List<String> roles) {
        return generateAccessToken(email, roles);
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Object roles = parseClaims(token).get("roles");
        if (roles instanceof List) return (List<String>) roles;
        return List.of();
    }

    /**
     * Get token type (access or refresh)
     */
    public String getTokenType(String token) {
        Object type = parseClaims(token).get("type");
        return type != null ? type.toString() : "unknown";
    }

    /**
     * Get token expiry time in milliseconds
     */
    public long getAccessTokenExpiryMs() {
        return ACCESS_TOKEN_EXPIRY_MS;
    }

    private Claims parseClaims(String token) {
        // use parserBuilder API for jjwt 0.11.x
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}