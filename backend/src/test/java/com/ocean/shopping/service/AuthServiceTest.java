package com.ocean.shopping.service;

import com.ocean.shopping.dto.auth.*;
import com.ocean.shopping.dto.user.UserResponse;
import com.ocean.shopping.exception.BadRequestException;
import com.ocean.shopping.exception.ConflictException;
import com.ocean.shopping.model.entity.User;
import com.ocean.shopping.model.entity.enums.UserRole;
import com.ocean.shopping.model.entity.enums.UserStatus;
import com.ocean.shopping.repository.UserRepository;
import com.ocean.shopping.security.CustomUserDetailsService;
import com.ocean.shopping.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private Authentication authentication;

    private AuthService authService;

    private User testUser;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                passwordEncoder,
                authenticationManager,
                tokenProvider,
                userDetailsService
        );

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

        registerRequest = new RegisterRequest(
                "newuser@example.com",
                "Password123",
                "Password123",
                "New",
                "User",
                "+1234567890",
                UserRole.CUSTOMER
        );

        loginRequest = new LoginRequest("test@example.com", "password", false);
    }

    @Test
    void register_WithValidRequest_ShouldReturnUserResponse() {
        // Given
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserResponse result = authService.register(registerRequest);

        // Then
        assertNotNull(result);
        assertEquals(testUser.getEmail(), result.getEmail());
        assertEquals(testUser.getFirstName(), result.getFirstName());
        assertEquals(testUser.getLastName(), result.getLastName());
        assertEquals(testUser.getRole(), result.getRole());

        verify(userRepository).existsByEmail(registerRequest.getEmail());
        verify(passwordEncoder).encode(registerRequest.getPassword());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_WithExistingEmail_ShouldThrowConflictException() {
        // Given
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        // When & Then
        ConflictException exception = assertThrows(ConflictException.class, () -> 
            authService.register(registerRequest));
        
        assertEquals("User already exists with email: " + registerRequest.getEmail(), 
                    exception.getMessage());
        
        verify(userRepository).existsByEmail(registerRequest.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_WithMismatchedPasswords_ShouldThrowBadRequestException() {
        // Given
        registerRequest.setConfirmPassword("DifferentPassword123");

        // When & Then
        BadRequestException exception = assertThrows(BadRequestException.class, () -> 
            authService.register(registerRequest));
        
        assertEquals("Passwords do not match", exception.getMessage());
        
        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_WithInvalidRole_ShouldThrowBadRequestException() {
        // Given
        registerRequest.setRole(UserRole.ADMINISTRATOR);

        // When & Then
        BadRequestException exception = assertThrows(BadRequestException.class, () -> 
            authService.register(registerRequest));
        
        assertTrue(exception.getMessage().contains("Invalid role for registration"));
        
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_WithValidCredentials_ShouldReturnLoginResponse() {
        // Given
        String accessToken = "access_token";
        String refreshToken = "refresh_token";
        
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(tokenProvider.generateAccessToken(testUser)).thenReturn(accessToken);
        when(tokenProvider.generateRefreshToken(testUser)).thenReturn(refreshToken);

        // When
        LoginResponse result = authService.login(loginRequest);

        // Then
        assertNotNull(result);
        assertEquals(accessToken, result.getAccessToken());
        assertEquals(refreshToken, result.getRefreshToken());
        assertEquals("Bearer", result.getTokenType());
        assertNotNull(result.getExpiresIn());
        assertNotNull(result.getUser());
        assertEquals(testUser.getEmail(), result.getUser().getEmail());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userDetailsService).updateLastLogin(testUser);
        verify(tokenProvider).generateAccessToken(testUser);
        verify(tokenProvider).generateRefreshToken(testUser);
    }

    @Test
    void login_WithInvalidCredentials_ShouldThrowBadRequestException() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // When & Then
        BadRequestException exception = assertThrows(BadRequestException.class, () -> 
            authService.login(loginRequest));
        
        assertEquals("Invalid email or password", exception.getMessage());
        
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(tokenProvider, never()).generateAccessToken(any(User.class));
        verify(tokenProvider, never()).generateRefreshToken(any(User.class));
    }

    @Test
    void refreshToken_WithValidToken_ShouldReturnNewAccessToken() {
        // Given
        String refreshToken = "valid_refresh_token";
        String newAccessToken = "new_access_token";
        String userId = testUser.getId().toString();
        
        RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);
        
        when(tokenProvider.getUserIdFromToken(refreshToken)).thenReturn(userId);
        when(tokenProvider.validateRefreshToken(refreshToken, userId)).thenReturn(true);
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(tokenProvider.generateAccessToken(testUser)).thenReturn(newAccessToken);

        // When
        RefreshTokenResponse result = authService.refreshToken(request);

        // Then
        assertNotNull(result);
        assertEquals(newAccessToken, result.getAccessToken());
        assertEquals("Bearer", result.getTokenType());
        assertNotNull(result.getExpiresIn());

        verify(tokenProvider).getUserIdFromToken(refreshToken);
        verify(tokenProvider).validateRefreshToken(refreshToken, userId);
        verify(userRepository).findById(testUser.getId());
        verify(tokenProvider).generateAccessToken(testUser);
    }

    @Test
    void refreshToken_WithInvalidToken_ShouldThrowBadRequestException() {
        // Given
        String refreshToken = "invalid_refresh_token";
        String userId = testUser.getId().toString();
        
        RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);
        
        when(tokenProvider.getUserIdFromToken(refreshToken)).thenReturn(userId);
        when(tokenProvider.validateRefreshToken(refreshToken, userId)).thenReturn(false);

        // When & Then
        BadRequestException exception = assertThrows(BadRequestException.class, () -> 
            authService.refreshToken(request));
        
        assertEquals("Invalid or expired refresh token", exception.getMessage());
        
        verify(tokenProvider).getUserIdFromToken(refreshToken);
        verify(tokenProvider).validateRefreshToken(refreshToken, userId);
        verify(userRepository, never()).findById(any(UUID.class));
    }

    @Test
    void refreshToken_WithUserNotFound_ShouldThrowBadRequestException() {
        // Given
        String refreshToken = "valid_refresh_token";
        String userId = testUser.getId().toString();
        
        RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);
        
        when(tokenProvider.getUserIdFromToken(refreshToken)).thenReturn(userId);
        when(tokenProvider.validateRefreshToken(refreshToken, userId)).thenReturn(true);
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.empty());

        // When & Then
        BadRequestException exception = assertThrows(BadRequestException.class, () -> 
            authService.refreshToken(request));
        
        assertEquals("User not found", exception.getMessage());
        
        verify(tokenProvider).getUserIdFromToken(refreshToken);
        verify(tokenProvider).validateRefreshToken(refreshToken, userId);
        verify(userRepository).findById(testUser.getId());
    }

    @Test
    void logout_WithValidTokenAndUserId_ShouldBlacklistTokenAndInvalidateRefreshToken() {
        // Given
        String accessToken = "valid_access_token";
        String userId = testUser.getId().toString();

        // When
        authService.logout(accessToken, userId);

        // Then
        verify(tokenProvider).blacklistToken(accessToken);
        verify(tokenProvider).invalidateRefreshToken(userId);
    }

    @Test
    void logout_WithNullAccessToken_ShouldStillInvalidateRefreshToken() {
        // Given
        String userId = testUser.getId().toString();

        // When
        authService.logout(null, userId);

        // Then
        verify(tokenProvider, never()).blacklistToken(anyString());
        verify(tokenProvider).invalidateRefreshToken(userId);
    }

    @Test
    void isUserActive_WithActiveUser_ShouldReturnTrue() {
        // Given
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));

        // When
        boolean result = authService.isUserActive(testUser.getEmail());

        // Then
        assertTrue(result);
        verify(userRepository).findByEmail(testUser.getEmail());
    }

    @Test
    void isUserActive_WithInactiveUser_ShouldReturnFalse() {
        // Given
        testUser.setStatus(UserStatus.INACTIVE);
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));

        // When
        boolean result = authService.isUserActive(testUser.getEmail());

        // Then
        assertFalse(result);
        verify(userRepository).findByEmail(testUser.getEmail());
    }

    @Test
    void isUserActive_WithNonExistentUser_ShouldReturnFalse() {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // When
        boolean result = authService.isUserActive("nonexistent@example.com");

        // Then
        assertFalse(result);
        verify(userRepository).findByEmail("nonexistent@example.com");
    }

    @Test
    void getUserByEmail_WithExistingUser_ShouldReturnUserResponse() {
        // Given
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));

        // When
        UserResponse result = authService.getUserByEmail(testUser.getEmail());

        // Then
        assertNotNull(result);
        assertEquals(testUser.getEmail(), result.getEmail());
        assertEquals(testUser.getFirstName(), result.getFirstName());
        assertEquals(testUser.getLastName(), result.getLastName());
        assertEquals(testUser.getRole(), result.getRole());

        verify(userRepository).findByEmail(testUser.getEmail());
    }

    @Test
    void getUserByEmail_WithNonExistentUser_ShouldThrowBadRequestException() {
        // Given
        String email = "nonexistent@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // When & Then
        BadRequestException exception = assertThrows(BadRequestException.class, () -> 
            authService.getUserByEmail(email));
        
        assertEquals("User not found", exception.getMessage());
        verify(userRepository).findByEmail(email);
    }
}