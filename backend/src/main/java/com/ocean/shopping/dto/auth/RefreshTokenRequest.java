package com.ocean.shopping.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Refresh token request DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Refresh token request")
public class RefreshTokenRequest {

    @Schema(description = "JWT refresh token", example = "eyJhbGciOiJIUzUxMiJ9...", required = true)
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}