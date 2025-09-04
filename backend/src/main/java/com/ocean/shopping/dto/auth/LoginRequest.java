package com.ocean.shopping.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Login request DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Login request")
public class LoginRequest {

    @Schema(description = "User email address", example = "user@example.com", required = true)
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @Schema(description = "User password", example = "password123", required = true)
    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    private String password;

    @Schema(description = "Remember me flag for extended session", example = "true")
    private boolean rememberMe = false;
}