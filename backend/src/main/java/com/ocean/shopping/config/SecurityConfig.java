package com.ocean.shopping.config;

import com.ocean.shopping.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Security configuration for JWT-based authentication
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;

    @Value("${ocean.shopping.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for JWT-based APIs
            .csrf(AbstractHttpConfigurer::disable)
            
            // Configure CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Set session management to stateless
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // Configure request authorization
            .authorizeHttpRequests(authz -> authz
                // Public endpoints
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/health/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                
                // Swagger/OpenAPI endpoints
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**").permitAll()
                
                // WebSocket endpoints
                .requestMatchers("/ws/**").permitAll()
                
                // Static resources
                .requestMatchers("/favicon.ico").permitAll()
                
                // Admin-only endpoints
                .requestMatchers("/api/admin/**").hasRole("ADMINISTRATOR")
                
                // Store owner endpoints
                .requestMatchers(HttpMethod.POST, "/api/stores/**").hasAnyRole("STORE_OWNER", "ADMINISTRATOR")
                .requestMatchers(HttpMethod.PUT, "/api/stores/**").hasAnyRole("STORE_OWNER", "ADMINISTRATOR")
                .requestMatchers(HttpMethod.DELETE, "/api/stores/**").hasAnyRole("STORE_OWNER", "ADMINISTRATOR")
                .requestMatchers(HttpMethod.GET, "/api/stores/*/management/**").hasAnyRole("STORE_OWNER", "ADMINISTRATOR")
                
                // Customer and above endpoints
                .requestMatchers("/api/user/**").hasAnyRole("CUSTOMER", "STORE_OWNER", "ADMINISTRATOR")
                .requestMatchers("/api/orders/**").hasAnyRole("CUSTOMER", "STORE_OWNER", "ADMINISTRATOR")
                .requestMatchers("/api/cart/**").hasAnyRole("CUSTOMER", "STORE_OWNER", "ADMINISTRATOR")
                
                // Public read-only endpoints
                .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/stores/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            
            // Add security headers
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.deny())
                .contentTypeOptions(contentTypeOptions -> {})
                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                    .maxAgeInSeconds(31536000)
                    .includeSubDomains(true)
                )
                .referrerPolicy(referrerPolicy -> referrerPolicy
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
            )
            
            // Set authentication provider
            .authenticationProvider(authenticationProvider())
            
            // Add JWT filter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Set allowed origins from application properties
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        configuration.setAllowedOriginPatterns(origins);
        
        // Set allowed methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        
        // Set allowed headers
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization", 
            "Content-Type", 
            "X-Requested-With", 
            "Accept", 
            "Origin", 
            "Access-Control-Request-Method", 
            "Access-Control-Request-Headers"
        ));
        
        // Set exposed headers
        configuration.setExposedHeaders(Arrays.asList(
            "Access-Control-Allow-Origin",
            "Access-Control-Allow-Credentials",
            "Authorization"
        ));
        
        // Allow credentials
        configuration.setAllowCredentials(true);
        
        // Set max age for preflight requests
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        source.registerCorsConfiguration("/ws/**", configuration);
        
        return source;
    }
}