package com.ocean.shopping.repository;

import com.ocean.shopping.model.entity.User;
import com.ocean.shopping.model.entity.enums.UserRole;
import com.ocean.shopping.model.entity.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for User entity
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByEmailVerificationToken(String token);
    
    Optional<User> findByPasswordResetToken(String token);
    
    List<User> findByRole(UserRole role);
    
    List<User> findByStatus(UserStatus status);
    
    List<User> findByRoleAndStatus(UserRole role, UserStatus status);
    
    boolean existsByEmail(String email);
    
    @Query("SELECT u FROM User u WHERE u.passwordResetToken IS NOT NULL AND u.passwordResetExpires < :now")
    List<User> findExpiredPasswordResetTokens(ZonedDateTime now);
    
    @Query("SELECT u FROM User u WHERE u.role = :role AND u.createdAt >= :since")
    List<User> findNewUsersByRole(UserRole role, ZonedDateTime since);
    
    long countByRole(UserRole role);
    
    long countByStatus(UserStatus status);
}