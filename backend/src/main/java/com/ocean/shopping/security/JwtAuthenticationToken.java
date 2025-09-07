package com.ocean.shopping.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.UUID;

/**
 * JWT Authentication Token for Spring Security
 */
public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    private final UUID userId;
    private final String email;
    private final Object principal;
    private String credentials;

    /**
     * Constructor for authenticated token
     */
    public JwtAuthenticationToken(UUID userId, String email, Object principal, 
                                  Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.userId = userId;
        this.email = email;
        this.principal = principal;
        this.credentials = null;
        setAuthenticated(true);
    }

    /**
     * Constructor for unauthenticated token
     */
    public JwtAuthenticationToken(String credentials) {
        super(null);
        this.userId = null;
        this.email = null;
        this.principal = null;
        this.credentials = credentials;
        setAuthenticated(false);
    }

    @Override
    public Object getCredentials() {
        return credentials;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public void eraseCredentials() {
        super.eraseCredentials();
        this.credentials = null;
    }
}