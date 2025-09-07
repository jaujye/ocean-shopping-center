package com.ocean.shopping.security;

import com.ocean.shopping.model.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * JWT token provider for generating and validating JWT tokens
 */
@Component
@Slf4j
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long jwtExpirationInMs;
    private final long refreshTokenExpirationInMs;
    private final StringRedisTemplate redisTemplate;

    public JwtTokenProvider(
            @Value("${ocean.shopping.jwt.secret}") String jwtSecret,
            @Value("${ocean.shopping.jwt.expiration}") long jwtExpirationInMs,
            @Value("${ocean.shopping.jwt.refresh-expiration}") long refreshTokenExpirationInMs,
            StringRedisTemplate redisTemplate) {
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        this.jwtExpirationInMs = jwtExpirationInMs;
        this.refreshTokenExpirationInMs = refreshTokenExpirationInMs;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Generate JWT access token from authentication
     */
    public String generateAccessToken(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return generateAccessToken(user);
    }

    /**
     * Generate JWT access token from user
     */
    public String generateAccessToken(User user) {
        Date expiryDate = new Date(System.currentTimeMillis() + jwtExpirationInMs);

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .claim("firstName", user.getFirstName())
                .claim("lastName", user.getLastName())
                .issuedAt(new Date())
                .expiration(expiryDate)
                .signWith(secretKey, Jwts.SIG.HS512)
                .compact();
    }

    /**
     * Generate refresh token
     */
    public String generateRefreshToken(User user) {
        Date expiryDate = new Date(System.currentTimeMillis() + refreshTokenExpirationInMs);

        String refreshToken = Jwts.builder()
                .subject(user.getId().toString())
                .claim("type", "refresh")
                .issuedAt(new Date())
                .expiration(expiryDate)
                .signWith(secretKey, Jwts.SIG.HS512)
                .compact();

        // Store refresh token in Redis with expiration
        String redisKey = "refresh_token:" + user.getId();
        redisTemplate.opsForValue().set(redisKey, refreshToken, 
            refreshTokenExpirationInMs, TimeUnit.MILLISECONDS);

        return refreshToken;
    }

    /**
     * Get user ID from JWT token
     */
    public String getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    /**
     * Get user email from JWT token
     */
    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.get("email", String.class);
    }

    /**
     * Get user role from JWT token
     */
    public String getRoleFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.get("role", String.class);
    }

    /**
     * Get token expiration date
     */
    public Date getExpirationDateFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getExpiration();
    }

    /**
     * Validate JWT token
     */
    public boolean validateToken(String authToken) {
        try {
            // Check if token is blacklisted
            if (isTokenBlacklisted(authToken)) {
                log.debug("Token is blacklisted");
                return false;
            }

            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(authToken);

            return true;
        } catch (SignatureException ex) {
            log.error("Invalid JWT signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * Validate refresh token
     */
    public boolean validateRefreshToken(String refreshToken, String userId) {
        try {
            // Check if token exists in Redis
            String redisKey = "refresh_token:" + userId;
            String storedToken = redisTemplate.opsForValue().get(redisKey);
            
            if (storedToken == null || !storedToken.equals(refreshToken)) {
                log.debug("Refresh token not found or doesn't match stored token");
                return false;
            }

            // Validate token signature and expiration
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(refreshToken)
                    .getPayload();

            // Check if it's actually a refresh token
            String tokenType = claims.get("type", String.class);
            if (!"refresh".equals(tokenType)) {
                log.debug("Token is not a refresh token");
                return false;
            }

            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            log.error("Invalid refresh token: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * Invalidate refresh token
     */
    public void invalidateRefreshToken(String userId) {
        String redisKey = "refresh_token:" + userId;
        redisTemplate.delete(redisKey);
    }

    /**
     * Blacklist access token for logout
     */
    public void blacklistToken(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            long ttl = expiration.getTime() - System.currentTimeMillis();
            
            if (ttl > 0) {
                String redisKey = "blacklist:" + token;
                redisTemplate.opsForValue().set(redisKey, "true", ttl, TimeUnit.MILLISECONDS);
            }
        } catch (Exception ex) {
            log.error("Error blacklisting token: {}", ex.getMessage());
        }
    }

    /**
     * Check if token is blacklisted
     */
    private boolean isTokenBlacklisted(String token) {
        try {
            String redisKey = "blacklist:" + token;
            return redisTemplate.hasKey(redisKey);
        } catch (Exception ex) {
            log.error("Error checking token blacklist: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * Get remaining TTL for token
     */
    public long getTokenTtl(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            return expiration.getTime() - System.currentTimeMillis();
        } catch (Exception ex) {
            return 0;
        }
    }
}