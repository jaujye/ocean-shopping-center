package com.ocean.shopping.dto.auth;

import com.ocean.shopping.model.entity.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User registration request DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User registration request")
public class RegisterRequest {

    @Schema(description = "User email address", example = "user@example.com", required = true)
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @Schema(description = "User password (minimum 6 characters)", example = "password123", required = true)
    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$", 
             message = "Password must contain at least one lowercase letter, one uppercase letter, and one digit")
    private String password;

    @Schema(description = "Password confirmation", example = "password123", required = true)
    @NotBlank(message = "Password confirmation is required")
    private String confirmPassword;

    @Schema(description = "User's first name", example = "John", required = true)
    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;

    @Schema(description = "User's last name", example = "Doe", required = true)
    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;

    @Schema(description = "User's phone number", example = "+1234567890")
    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    @Pattern(regexp = "^[+]?[1-9]\\d{1,14}$", message = "Phone number should be valid")
    private String phone;

    @Schema(description = "User role", example = "CUSTOMER", allowableValues = {"CUSTOMER", "STORE_OWNER"})
    private UserRole role = UserRole.CUSTOMER;

    /**
     * Validate that passwords match
     */
    public boolean isPasswordMatching() {
        return password != null && password.equals(confirmPassword);
    }
}