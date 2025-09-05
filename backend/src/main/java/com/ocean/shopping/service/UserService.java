package com.ocean.shopping.service;

import com.ocean.shopping.dto.user.UpdateProfileRequest;
import com.ocean.shopping.dto.user.UserResponse;
import com.ocean.shopping.exception.BadRequestException;
import com.ocean.shopping.exception.ResourceNotFoundException;
import com.ocean.shopping.model.entity.User;
import com.ocean.shopping.model.entity.enums.UserRole;
import com.ocean.shopping.model.entity.enums.UserStatus;
import com.ocean.shopping.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * User management service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Get user profile by ID
     */
    @Transactional(readOnly = true)
    public UserResponse getUserProfile(UUID userId) {
        log.debug("Getting user profile for ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        return UserResponse.fromEntity(user);
    }

    /**
     * Update user profile
     */
    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        log.info("Updating profile for user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Update only non-null fields
        if (StringUtils.hasText(request.getFirstName())) {
            user.setFirstName(request.getFirstName().trim());
        }

        if (StringUtils.hasText(request.getLastName())) {
            user.setLastName(request.getLastName().trim());
        }

        if (StringUtils.hasText(request.getPhone())) {
            user.setPhone(request.getPhone().trim());
        }

        if (StringUtils.hasText(request.getAvatarUrl())) {
            user.setAvatarUrl(request.getAvatarUrl().trim());
        }

        User updatedUser = userRepository.save(user);
        log.info("Successfully updated profile for user: {}", updatedUser.getEmail());

        return UserResponse.fromEntity(updatedUser);
    }

    /**
     * Change user password
     */
    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        log.info("Changing password for user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }

        // Validate new password strength
        if (newPassword.length() < 6) {
            throw new BadRequestException("New password must be at least 6 characters long");
        }

        // Update password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("Successfully changed password for user: {}", user.getEmail());
    }

    /**
     * Deactivate user account
     */
    @Transactional
    public void deactivateAccount(UUID userId) {
        log.info("Deactivating account for user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);

        log.info("Successfully deactivated account for user: {}", user.getEmail());
    }

    /**
     * Get all users (Admin only)
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        log.debug("Getting all users with pagination");

        Page<User> users = userRepository.findAll(pageable);
        return users.map(UserResponse::fromEntity);
    }

    /**
     * Get users by role (Admin only)
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public List<UserResponse> getUsersByRole(UserRole role) {
        log.debug("Getting users by role: {}", role);

        List<User> users = userRepository.findByRole(role);
        return users.stream()
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get users by status (Admin only)
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public List<UserResponse> getUsersByStatus(UserStatus status) {
        log.debug("Getting users by status: {}", status);

        List<User> users = userRepository.findByStatus(status);
        return users.stream()
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Update user role (Admin only)
     */
    @Transactional
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public UserResponse updateUserRole(UUID userId, UserRole newRole) {
        log.info("Updating role for user ID: {} to role: {}", userId, newRole);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        UserRole oldRole = user.getRole();
        user.setRole(newRole);
        User updatedUser = userRepository.save(user);

        log.info("Successfully updated user {} role from {} to {}", 
                updatedUser.getEmail(), oldRole, newRole);

        return UserResponse.fromEntity(updatedUser);
    }

    /**
     * Update user status (Admin only)
     */
    @Transactional
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public UserResponse updateUserStatus(UUID userId, UserStatus newStatus) {
        log.info("Updating status for user ID: {} to status: {}", userId, newStatus);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        UserStatus oldStatus = user.getStatus();
        user.setStatus(newStatus);
        User updatedUser = userRepository.save(user);

        log.info("Successfully updated user {} status from {} to {}", 
                updatedUser.getEmail(), oldStatus, newStatus);

        return UserResponse.fromEntity(updatedUser);
    }

    /**
     * Delete user account (Admin only)
     */
    @Transactional
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public void deleteUser(UUID userId) {
        log.info("Deleting user with ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        userRepository.delete(user);
        log.info("Successfully deleted user: {}", user.getEmail());
    }

    /**
     * Get user statistics (Admin only)
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public UserStatistics getUserStatistics() {
        log.debug("Getting user statistics");

        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByStatus(UserStatus.ACTIVE);
        long customers = userRepository.countByRole(UserRole.CUSTOMER);
        long storeOwners = userRepository.countByRole(UserRole.STORE_OWNER);
        long administrators = userRepository.countByRole(UserRole.ADMINISTRATOR);

        return UserStatistics.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .inactiveUsers(totalUsers - activeUsers)
                .customers(customers)
                .storeOwners(storeOwners)
                .administrators(administrators)
                .build();
    }

    /**
     * Get new users in the last period (Admin only)
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public List<UserResponse> getNewUsers(int days) {
        log.debug("Getting new users from last {} days", days);

        ZonedDateTime since = ZonedDateTime.now().minusDays(days);
        List<User> newUsers = userRepository.findNewUsersByRole(null, since);

        return newUsers.stream()
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * User statistics DTO
     */
    @lombok.Builder
    @lombok.Data
    public static class UserStatistics {
        private long totalUsers;
        private long activeUsers;
        private long inactiveUsers;
        private long customers;
        private long storeOwners;
        private long administrators;
    }
}