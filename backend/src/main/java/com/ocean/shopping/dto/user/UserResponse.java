package com.ocean.shopping.dto.user;

import com.ocean.shopping.model.entity.User;
import com.ocean.shopping.model.entity.enums.UserRole;
import com.ocean.shopping.model.entity.enums.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * User response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User information response")
public class UserResponse {

    @Schema(description = "User unique identifier", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Schema(description = "User email address", example = "user@example.com")
    private String email;

    @Schema(description = "User's first name", example = "John")
    private String firstName;

    @Schema(description = "User's last name", example = "Doe")
    private String lastName;

    @Schema(description = "User's full name", example = "John Doe")
    private String fullName;

    @Schema(description = "User's phone number", example = "+1234567890")
    private String phone;

    @Schema(description = "User role", example = "CUSTOMER")
    private UserRole role;

    @Schema(description = "User account status", example = "ACTIVE")
    private UserStatus status;

    @Schema(description = "Email verification status", example = "true")
    private Boolean emailVerified;

    @Schema(description = "User's avatar URL", example = "https://example.com/avatar.jpg")
    private String avatarUrl;

    @Schema(description = "Last login timestamp", example = "2023-12-01T10:30:00Z")
    private ZonedDateTime lastLogin;

    @Schema(description = "Account creation timestamp", example = "2023-11-01T09:00:00Z")
    private ZonedDateTime createdAt;

    @Schema(description = "Account last update timestamp", example = "2023-12-01T10:30:00Z")
    private ZonedDateTime updatedAt;

    /**
     * Create UserResponse from User entity
     */
    public static UserResponse fromEntity(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .role(user.getRole())
                .status(user.getStatus())
                .emailVerified(user.getEmailVerified())
                .avatarUrl(user.getAvatarUrl())
                .lastLogin(user.getLastLogin())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}