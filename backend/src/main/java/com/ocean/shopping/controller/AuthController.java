package com.ocean.shopping.controller;

import com.ocean.shopping.dto.auth.*;
import com.ocean.shopping.dto.user.UserResponse;
import com.ocean.shopping.exception.ErrorResponse;
import com.ocean.shopping.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication controller handling user registration, login, logout, and token refresh
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Authentication and authorization endpoints")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Register a new user", 
               description = "Register a new user account with email, password, and profile information")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "User registered successfully",
                     content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "User already exists",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration request received for email: {}", request.getEmail());

        UserResponse userResponse = authService.register(request);
        
        log.info("User registered successfully: {}", userResponse.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(userResponse);
    }

    @Operation(summary = "User login", 
               description = "Authenticate user and return JWT access and refresh tokens")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Login successful",
                     content = @Content(schema = @Schema(implementation = LoginResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid credentials or account disabled",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Authentication failed",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request received for email: {}", request.getEmail());

        LoginResponse loginResponse = authService.login(request);
        
        log.info("User logged in successfully: {}", loginResponse.getUser().getEmail());
        return ResponseEntity.ok(loginResponse);
    }

    @Operation(summary = "Refresh access token", 
               description = "Generate a new access token using a valid refresh token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token refreshed successfully",
                     content = @Content(schema = @Schema(implementation = RefreshTokenResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid or expired refresh token",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Refresh token validation failed",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.debug("Token refresh request received");

        RefreshTokenResponse refreshResponse = authService.refreshToken(request);
        
        log.debug("Token refreshed successfully");
        return ResponseEntity.ok(refreshResponse);
    }

    @Operation(summary = "User logout", 
               description = "Logout user and invalidate access and refresh tokens")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Logout successful"),
        @ApiResponse(responseCode = "401", description = "User not authenticated",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.getPrincipal() instanceof com.ocean.shopping.model.entity.User) {
            com.ocean.shopping.model.entity.User user = 
                (com.ocean.shopping.model.entity.User) authentication.getPrincipal();
            
            // Extract access token from request
            String accessToken = extractTokenFromRequest(request);
            
            log.info("Logout request received for user: {}", user.getEmail());
            
            authService.logout(accessToken, user.getId().toString());
            
            log.info("User logged out successfully: {}", user.getEmail());
        }

        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Validate token", 
               description = "Validate if the current access token is valid and return user info")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token is valid",
                     content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "401", description = "Token is invalid or expired",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/validate")
    public ResponseEntity<UserResponse> validateToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.getPrincipal() instanceof com.ocean.shopping.model.entity.User) {
            com.ocean.shopping.model.entity.User user = 
                (com.ocean.shopping.model.entity.User) authentication.getPrincipal();
            
            log.debug("Token validation successful for user: {}", user.getEmail());
            return ResponseEntity.ok(UserResponse.fromEntity(user));
        }
        
        log.debug("Token validation failed - no authenticated user");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @Operation(summary = "Check if user exists", 
               description = "Check if a user exists with the given email address")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User existence check completed"),
        @ApiResponse(responseCode = "400", description = "Invalid email format",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/check-user")
    public ResponseEntity<CheckUserResponse> checkUser(@RequestParam String email) {
        log.debug("Checking user existence for email: {}", email);

        boolean exists = authService.isUserActive(email);
        
        CheckUserResponse response = CheckUserResponse.builder()
                .exists(exists)
                .email(email)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Extract JWT token from Authorization header
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * Check user existence response DTO
     */
    @lombok.Builder
    @lombok.Data
    @Schema(description = "Check user existence response")
    public static class CheckUserResponse {
        @Schema(description = "Whether user exists", example = "true")
        private boolean exists;
        
        @Schema(description = "Email address checked", example = "user@example.com")
        private String email;
    }
}