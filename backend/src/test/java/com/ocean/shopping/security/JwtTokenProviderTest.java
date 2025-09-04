package com.ocean.shopping.security;

import com.ocean.shopping.model.entity.User;
import com.ocean.shopping.model.entity.enums.UserRole;
import com.ocean.shopping.model.entity.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private JwtTokenProvider jwtTokenProvider;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Initialize JwtTokenProvider with test values - need at least 256 bits (32 bytes) for HS512
        String testSecret = "ocean_shopping_test_jwt_secret_key_2024_needs_to_be_at_least_32_bytes_long_for_hs512_algorithm";
        jwtTokenProvider = new JwtTokenProvider(
                testSecret,
                3600000, // 1 hour
                86400000, // 1 day
                redisTemplate
        );

        // Mock Redis template operations
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.hasKey(anyString())).thenReturn(false);

        // Create test user
        testUser = User.builder()
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .passwordHash("encoded_password")
                .role(UserRole.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build();
        testUser.setId(UUID.randomUUID());
    }

    @Test
    void generateAccessToken_WithUser_ShouldReturnValidToken() {
        // When
        String token = jwtTokenProvider.generateAccessToken(testUser);

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.length() > 50); // JWT tokens are typically long
        assertTrue(jwtTokenProvider.validateToken(token));
    }

    @Test
    void generateAccessToken_WithAuthentication_ShouldReturnValidToken() {
        // Given
        Authentication auth = new UsernamePasswordAuthenticationToken(
                testUser, null, testUser.getAuthorities());

        // When
        String token = jwtTokenProvider.generateAccessToken(auth);

        // Then
        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
    }

    @Test
    void generateRefreshToken_WithUser_ShouldReturnValidTokenAndStoreInRedis() {
        // When
        String refreshToken = jwtTokenProvider.generateRefreshToken(testUser);

        // Then
        assertNotNull(refreshToken);
        assertTrue(refreshToken.length() > 50);
        
        // Verify Redis storage
        verify(valueOperations).set(
                eq("refresh_token:" + testUser.getId()),
                eq(refreshToken),
                eq(86400000L),
                eq(TimeUnit.MILLISECONDS)
        );
    }

    @Test
    void getUserIdFromToken_WithValidToken_ShouldReturnUserId() {
        // Given
        String token = jwtTokenProvider.generateAccessToken(testUser);

        // When
        String userId = jwtTokenProvider.getUserIdFromToken(token);

        // Then
        assertEquals(testUser.getId().toString(), userId);
    }

    @Test
    void getEmailFromToken_WithValidToken_ShouldReturnEmail() {
        // Given
        String token = jwtTokenProvider.generateAccessToken(testUser);

        // When
        String email = jwtTokenProvider.getEmailFromToken(token);

        // Then
        assertEquals(testUser.getEmail(), email);
    }

    @Test
    void getRoleFromToken_WithValidToken_ShouldReturnRole() {
        // Given
        String token = jwtTokenProvider.generateAccessToken(testUser);

        // When
        String role = jwtTokenProvider.getRoleFromToken(token);

        // Then
        assertEquals(testUser.getRole().name(), role);
    }

    @Test
    void getExpirationDateFromToken_WithValidToken_ShouldReturnFutureDate() {
        // Given
        String token = jwtTokenProvider.generateAccessToken(testUser);

        // When
        Date expirationDate = jwtTokenProvider.getExpirationDateFromToken(token);

        // Then
        assertNotNull(expirationDate);
        assertTrue(expirationDate.after(new Date()));
    }

    @Test
    void validateToken_WithValidToken_ShouldReturnTrue() {
        // Given
        String token = jwtTokenProvider.generateAccessToken(testUser);

        // When
        boolean isValid = jwtTokenProvider.validateToken(token);

        // Then
        assertTrue(isValid);
    }

    @Test
    void validateToken_WithInvalidToken_ShouldReturnFalse() {
        // Given
        String invalidToken = "invalid.jwt.token";

        // When
        boolean isValid = jwtTokenProvider.validateToken(invalidToken);

        // Then
        assertFalse(isValid);
    }

    @Test
    void validateToken_WithBlacklistedToken_ShouldReturnFalse() {
        // Given
        String token = jwtTokenProvider.generateAccessToken(testUser);
        when(redisTemplate.hasKey("blacklist:" + token)).thenReturn(true);

        // When
        boolean isValid = jwtTokenProvider.validateToken(token);

        // Then
        assertFalse(isValid);
    }

    @Test
    void validateRefreshToken_WithValidTokenAndStoredInRedis_ShouldReturnTrue() {
        // Given
        String refreshToken = jwtTokenProvider.generateRefreshToken(testUser);
        String userId = testUser.getId().toString();
        when(valueOperations.get("refresh_token:" + userId)).thenReturn(refreshToken);

        // When
        boolean isValid = jwtTokenProvider.validateRefreshToken(refreshToken, userId);

        // Then
        assertTrue(isValid);
    }

    @Test
    void validateRefreshToken_WithTokenNotInRedis_ShouldReturnFalse() {
        // Given
        String refreshToken = jwtTokenProvider.generateRefreshToken(testUser);
        String userId = testUser.getId().toString();
        when(valueOperations.get("refresh_token:" + userId)).thenReturn(null);

        // When
        boolean isValid = jwtTokenProvider.validateRefreshToken(refreshToken, userId);

        // Then
        assertFalse(isValid);
    }

    @Test
    void invalidateRefreshToken_WithUserId_ShouldDeleteFromRedis() {
        // Given
        String userId = testUser.getId().toString();

        // When
        jwtTokenProvider.invalidateRefreshToken(userId);

        // Then
        verify(redisTemplate).delete("refresh_token:" + userId);
    }

    @Test
    void blacklistToken_WithValidToken_ShouldStoreInRedisWithTtl() {
        // Given
        String token = jwtTokenProvider.generateAccessToken(testUser);

        // When
        jwtTokenProvider.blacklistToken(token);

        // Then
        verify(valueOperations).set(
                eq("blacklist:" + token),
                eq("true"),
                anyLong(),
                eq(TimeUnit.MILLISECONDS)
        );
    }

    @Test
    void getTokenTtl_WithValidToken_ShouldReturnPositiveValue() {
        // Given
        String token = jwtTokenProvider.generateAccessToken(testUser);

        // When
        long ttl = jwtTokenProvider.getTokenTtl(token);

        // Then
        assertTrue(ttl > 0);
        assertTrue(ttl <= 3600000); // Should be less than or equal to expiration time
    }

    @Test
    void getTokenTtl_WithInvalidToken_ShouldReturnZero() {
        // Given
        String invalidToken = "invalid.jwt.token";

        // When
        long ttl = jwtTokenProvider.getTokenTtl(invalidToken);

        // Then
        assertEquals(0, ttl);
    }
}