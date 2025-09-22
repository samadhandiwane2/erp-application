package com.erp.common.jwt;

import com.erp.common.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenValidityMs;
    private final long refreshTokenValidityMs;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-validity:900000}") long accessTokenValidityMs, // 15 minutes
            @Value("${app.jwt.refresh-token-validity:86400000}") long refreshTokenValidityMs // 24 hours
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenValidityMs = accessTokenValidityMs;
        this.refreshTokenValidityMs = refreshTokenValidityMs;
    }

    public String generateAccessToken(UserPrincipal userPrincipal) {
        return generateToken(userPrincipal, accessTokenValidityMs, "access");
    }

    public String generateRefreshToken(UserPrincipal userPrincipal) {
        return generateToken(userPrincipal, refreshTokenValidityMs, "refresh");
    }

    private String generateToken(UserPrincipal userPrincipal, long validityMs, String tokenType) {
        Instant now = Instant.now();
        Instant expiryDate = now.plus(validityMs, ChronoUnit.MILLIS);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userPrincipal.getId());
        claims.put("username", userPrincipal.getUsername());
        claims.put("email", userPrincipal.getEmail());
        claims.put("userType", userPrincipal.getUserType().name());
        claims.put("tenantId", userPrincipal.getTenantId());
        claims.put("tenantCode", userPrincipal.getTenantCode());
        claims.put("tokenType", tokenType);

        return Jwts.builder()
                .claims(claims)
                .subject(userPrincipal.getUsername())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiryDate))
                .signWith(secretKey)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getSubject();
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("userId", Long.class);
    }

    public Long getTenantIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("tenantId", Long.class);
    }

    public String getTenantCodeFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("tenantCode", String.class);
    }

    public User.UserType getUserTypeFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        String userTypeStr = claims.get("userType", String.class);
        return User.UserType.valueOf(userTypeStr);
    }

    public String getTokenTypeFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("tokenType", String.class);
    }

    public boolean validateToken(String token) {
        try {
            getClaimsFromToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.getExpiration().before(new Date());
        } catch (JwtException e) {
            return true;
        }
    }

    private Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getAccessTokenValidityMs() {
        return accessTokenValidityMs;
    }
}
