package com.ocean.shopping.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Update user profile request DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Update user profile request")
public class UpdateProfileRequest {

    @Schema(description = "User's first name", example = "John")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;

    @Schema(description = "User's last name", example = "Doe")
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;

    @Schema(description = "User's phone number", example = "+1234567890")
    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    @Pattern(regexp = "^[+]?[1-9]\\d{1,14}$", message = "Phone number should be valid")
    private String phone;

    @Schema(description = "User's avatar URL", example = "https://example.com/avatar.jpg")
    @Size(max = 500, message = "Avatar URL must not exceed 500 characters")
    private String avatarUrl;
}