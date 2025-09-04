package com.ocean.shopping.controller;

import com.ocean.shopping.dto.user.UpdateProfileRequest;
import com.ocean.shopping.dto.user.UserResponse;
import com.ocean.shopping.exception.ErrorResponse;
import com.ocean.shopping.model.entity.User;
import com.ocean.shopping.model.entity.enums.UserRole;
import com.ocean.shopping.model.entity.enums.UserStatus;
import com.ocean.shopping.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * User management controller for profile operations
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Management", description = "User profile and management endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Get current user profile", 
               description = "Get the profile information of the currently authenticated user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Profile retrieved successfully",
                     content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "401", description = "User not authenticated",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "User profile not found",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getCurrentUserProfile() {
        User currentUser = getCurrentUser();
        log.debug("Getting profile for user: {}", currentUser.getEmail());

        UserResponse userResponse = userService.getUserProfile(currentUser.getId());
        return ResponseEntity.ok(userResponse);
    }

    @Operation(summary = "Update current user profile", 
               description = "Update profile information of the currently authenticated user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Profile updated successfully",
                     content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "User not authenticated",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "User profile not found",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/profile")
    public ResponseEntity<UserResponse> updateCurrentUserProfile(@Valid @RequestBody UpdateProfileRequest request) {
        User currentUser = getCurrentUser();
        log.info("Updating profile for user: {}", currentUser.getEmail());

        UserResponse userResponse = userService.updateProfile(currentUser.getId(), request);
        return ResponseEntity.ok(userResponse);
    }

    @Operation(summary = "Change password", 
               description = "Change password for the currently authenticated user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Password changed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid current password or new password",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "User not authenticated",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        User currentUser = getCurrentUser();
        log.info("Changing password for user: {}", currentUser.getEmail());

        userService.changePassword(currentUser.getId(), request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Deactivate account", 
               description = "Deactivate the currently authenticated user's account")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Account deactivated successfully"),
        @ApiResponse(responseCode = "401", description = "User not authenticated",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/profile")
    public ResponseEntity<Void> deactivateAccount() {
        User currentUser = getCurrentUser();
        log.info("Deactivating account for user: {}", currentUser.getEmail());

        userService.deactivateAccount(currentUser.getId());
        return ResponseEntity.ok().build();
    }

    // Admin-only endpoints

    @Operation(summary = "Get all users (Admin only)", 
               description = "Get paginated list of all users in the system")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "User not authenticated",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Insufficient privileges",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @PageableDefault(size = 20) Pageable pageable) {
        log.debug("Getting all users with pagination");

        Page<UserResponse> users = userService.getAllUsers(pageable);
        return ResponseEntity.ok(users);
    }

    @Operation(summary = "Get users by role (Admin only)", 
               description = "Get all users with a specific role")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "User not authenticated",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Insufficient privileges",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/admin/users/role/{role}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<List<UserResponse>> getUsersByRole(
            @Parameter(description = "User role", example = "CUSTOMER")
            @PathVariable UserRole role) {
        log.debug("Getting users by role: {}", role);

        List<UserResponse> users = userService.getUsersByRole(role);
        return ResponseEntity.ok(users);
    }

    @Operation(summary = "Get users by status (Admin only)", 
               description = "Get all users with a specific status")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "User not authenticated",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Insufficient privileges",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/admin/users/status/{status}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<List<UserResponse>> getUsersByStatus(
            @Parameter(description = "User status", example = "ACTIVE")
            @PathVariable UserStatus status) {
        log.debug("Getting users by status: {}", status);

        List<UserResponse> users = userService.getUsersByStatus(status);
        return ResponseEntity.ok(users);
    }

    @Operation(summary = "Update user role (Admin only)", 
               description = "Update the role of a specific user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User role updated successfully",
                     content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid role or user ID",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "User not authenticated",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Insufficient privileges",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "User not found",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/admin/users/{userId}/role")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<UserResponse> updateUserRole(
            @Parameter(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateRoleRequest request) {
        log.info("Updating role for user ID: {} to role: {}", userId, request.getRole());

        UserResponse userResponse = userService.updateUserRole(userId, request.getRole());
        return ResponseEntity.ok(userResponse);
    }

    @Operation(summary = "Update user status (Admin only)", 
               description = "Update the status of a specific user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User status updated successfully",
                     content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid status or user ID",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "User not authenticated",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Insufficient privileges",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "User not found",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/admin/users/{userId}/status")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<UserResponse> updateUserStatus(
            @Parameter(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateStatusRequest request) {
        log.info("Updating status for user ID: {} to status: {}", userId, request.getStatus());

        UserResponse userResponse = userService.updateUserStatus(userId, request.getStatus());
        return ResponseEntity.ok(userResponse);
    }

    @Operation(summary = "Get user statistics (Admin only)", 
               description = "Get system-wide user statistics")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "User not authenticated",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Insufficient privileges",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/admin/statistics")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<UserService.UserStatistics> getUserStatistics() {
        log.debug("Getting user statistics");

        UserService.UserStatistics statistics = userService.getUserStatistics();
        return ResponseEntity.ok(statistics);
    }

    /**
     * Get current authenticated user
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (User) authentication.getPrincipal();
    }

    /**
     * Change password request DTO
     */
    @lombok.Data
    @Schema(description = "Change password request")
    public static class ChangePasswordRequest {
        @Schema(description = "Current password", example = "oldpassword123", required = true)
        @jakarta.validation.constraints.NotBlank(message = "Current password is required")
        private String currentPassword;

        @Schema(description = "New password", example = "newpassword123", required = true)
        @jakarta.validation.constraints.NotBlank(message = "New password is required")
        @jakarta.validation.constraints.Size(min = 6, message = "New password must be at least 6 characters long")
        private String newPassword;
    }

    /**
     * Update role request DTO
     */
    @lombok.Data
    @Schema(description = "Update user role request")
    public static class UpdateRoleRequest {
        @Schema(description = "New user role", example = "STORE_OWNER", required = true)
        @jakarta.validation.constraints.NotNull(message = "Role is required")
        private UserRole role;
    }

    /**
     * Update status request DTO
     */
    @lombok.Data
    @Schema(description = "Update user status request")
    public static class UpdateStatusRequest {
        @Schema(description = "New user status", example = "ACTIVE", required = true)
        @jakarta.validation.constraints.NotNull(message = "Status is required")
        private UserStatus status;
    }
}