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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Authentication service for user registration, login, and token management
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;

    @Value("${ocean.shopping.jwt.expiration}")
    private int jwtExpirationInMs;

    /**
     * Register a new user
     */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        log.info("Attempting to register user with email: {}", request.getEmail());

        // Validate password confirmation
        if (!request.isPasswordMatching()) {
            throw new BadRequestException("Passwords do not match");
        }

        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("User already exists with email: " + request.getEmail());
        }

        // Validate role - only CUSTOMER and STORE_OWNER can self-register
        if (request.getRole() != UserRole.CUSTOMER && request.getRole() != UserRole.STORE_OWNER) {
            throw new BadRequestException("Invalid role for registration. Only CUSTOMER and STORE_OWNER are allowed.");
        }

        // Create and save user
        User user = User.builder()
                .email(request.getEmail().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .phone(request.getPhone() != null ? request.getPhone().trim() : null)
                .role(request.getRole())
                .status(UserStatus.ACTIVE)
                .emailVerified(false) // Email verification would be implemented later
                .build();

        User savedUser = userRepository.save(user);
        log.info("Successfully registered user: {} with role: {}", savedUser.getEmail(), savedUser.getRole());

        return UserResponse.fromEntity(savedUser);
    }

    /**
     * Authenticate user and generate tokens
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        log.info("Attempting login for email: {}", request.getEmail());

        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail().toLowerCase().trim(),
                            request.getPassword()
                    )
            );

            User user = (User) authentication.getPrincipal();

            // Check if account is active
            if (user.getStatus() != UserStatus.ACTIVE) {
                throw new DisabledException("User account is not active");
            }

            // Update last login
            userDetailsService.updateLastLogin(user);

            // Generate tokens
            String accessToken = tokenProvider.generateAccessToken(user);
            String refreshToken = tokenProvider.generateRefreshToken(user);

            log.info("Successfully authenticated user: {} with role: {}", user.getEmail(), user.getRole());

            return LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn((long) jwtExpirationInMs / 1000) // Convert to seconds
                    .user(UserResponse.fromEntity(user))
                    .build();

        } catch (BadCredentialsException ex) {
            log.warn("Failed login attempt for email: {}", request.getEmail());
            throw new BadRequestException("Invalid email or password");
        } catch (DisabledException ex) {
            log.warn("Login attempt for disabled account: {}", request.getEmail());
            throw new BadRequestException("Account is disabled");
        }
    }

    /**
     * Refresh access token using refresh token
     */
    @Transactional
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        log.debug("Attempting to refresh token");

        try {
            String refreshToken = request.getRefreshToken();
            String userId = tokenProvider.getUserIdFromToken(refreshToken);

            // Validate refresh token
            if (!tokenProvider.validateRefreshToken(refreshToken, userId)) {
                throw new BadRequestException("Invalid or expired refresh token");
            }

            // Load user and generate new access token
            User user = userRepository.findById(UUID.fromString(userId))
                    .orElseThrow(() -> new BadRequestException("User not found"));

            if (user.getStatus() != UserStatus.ACTIVE) {
                throw new BadRequestException("User account is not active");
            }

            String newAccessToken = tokenProvider.generateAccessToken(user);

            log.debug("Successfully refreshed token for user: {}", user.getEmail());

            return RefreshTokenResponse.builder()
                    .accessToken(newAccessToken)
                    .tokenType("Bearer")
                    .expiresIn((long) jwtExpirationInMs / 1000)
                    .build();

        } catch (Exception ex) {
            log.error("Failed to refresh token", ex);
            throw new BadRequestException("Invalid refresh token");
        }
    }

    /**
     * Logout user and invalidate tokens
     */
    @Transactional
    public void logout(String accessToken, String userId) {
        log.info("Logging out user: {}", userId);

        try {
            // Blacklist the access token
            if (accessToken != null) {
                tokenProvider.blacklistToken(accessToken);
            }

            // Invalidate refresh token
            tokenProvider.invalidateRefreshToken(userId);

            log.info("Successfully logged out user: {}", userId);

        } catch (Exception ex) {
            log.error("Error during logout for user: {}", userId, ex);
            // Don't throw exception - logout should always succeed
        }
    }

    /**
     * Validate if user exists and is active
     */
    @Transactional(readOnly = true)
    public boolean isUserActive(String email) {
        return userRepository.findByEmail(email)
                .map(user -> user.getStatus() == UserStatus.ACTIVE)
                .orElse(false);
    }

    /**
     * Get user by email
     */
    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));

        return UserResponse.fromEntity(user);
    }
}