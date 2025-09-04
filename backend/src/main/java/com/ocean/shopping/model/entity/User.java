package com.ocean.shopping.model.entity;

import com.ocean.shopping.model.entity.enums.UserRole;
import com.ocean.shopping.model.entity.enums.UserStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * User entity representing users table with role-based access control
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_email", columnList = "email"),
    @Index(name = "idx_users_role", columnList = "role"),
    @Index(name = "idx_users_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity implements UserDetails {

    @Column(name = "email", unique = true, nullable = false)
    @Email(message = "Email should be valid")
    @NotBlank(message = "Email is required")
    @Size(max = 255)
    private String email;

    @Column(name = "password_hash", nullable = false)
    @NotBlank(message = "Password is required")
    @Size(max = 255)
    private String passwordHash;

    @Column(name = "first_name", nullable = false)
    @NotBlank(message = "First name is required")
    @Size(max = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    @NotBlank(message = "Last name is required")
    @Size(max = 100)
    private String lastName;

    @Column(name = "phone")
    @Size(max = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    @Builder.Default
    private UserRole role = UserRole.CUSTOMER;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private Boolean emailVerified = false;

    @Column(name = "email_verification_token")
    @Size(max = 255)
    private String emailVerificationToken;

    @Column(name = "password_reset_token")
    @Size(max = 255)
    private String passwordResetToken;

    @Column(name = "password_reset_expires")
    private ZonedDateTime passwordResetExpires;

    @Column(name = "last_login")
    private ZonedDateTime lastLogin;

    @Column(name = "avatar_url")
    @Size(max = 500)
    private String avatarUrl;

    // Relationships
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<UserAddress> addresses;

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Store> ownedStores;

    // Spring Security UserDetails implementation
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return status != UserStatus.SUSPENDED;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != UserStatus.SUSPENDED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE;
    }

    // Helper methods
    public String getFullName() {
        return firstName + " " + lastName;
    }

    public boolean isCustomer() {
        return role == UserRole.CUSTOMER;
    }

    public boolean isStoreOwner() {
        return role == UserRole.STORE_OWNER;
    }

    public boolean isAdministrator() {
        return role == UserRole.ADMINISTRATOR;
    }
}