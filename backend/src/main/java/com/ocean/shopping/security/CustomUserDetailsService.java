package com.ocean.shopping.security;

import com.ocean.shopping.model.entity.User;
import com.ocean.shopping.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;

/**
 * Custom UserDetailsService implementation for Spring Security
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("Loading user by email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        String.format("User not found with email: %s", email)));

        log.debug("Successfully loaded user: {} with role: {}", user.getEmail(), user.getRole());
        
        return user;
    }

    /**
     * Load user by ID for JWT token validation
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserById(String userId) throws UsernameNotFoundException {
        log.debug("Loading user by ID: {}", userId);

        try {
            User user = userRepository.findById(java.util.UUID.fromString(userId))
                    .orElseThrow(() -> new UsernameNotFoundException(
                            String.format("User not found with id: %s", userId)));

            log.debug("Successfully loaded user by ID: {} with email: {}", userId, user.getEmail());
            
            return user;
        } catch (IllegalArgumentException ex) {
            log.error("Invalid UUID format: {}", userId);
            throw new UsernameNotFoundException(String.format("Invalid user id format: %s", userId));
        }
    }

    /**
     * Update user last login timestamp
     */
    @Transactional
    public void updateLastLogin(User user) {
        try {
            user.setLastLogin(ZonedDateTime.now());
            userRepository.save(user);
            log.debug("Updated last login for user: {}", user.getEmail());
        } catch (Exception ex) {
            log.error("Failed to update last login for user: {}", user.getEmail(), ex);
        }
    }
}