package com.ocean.shopping.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Test-specific database configuration
 */
@TestConfiguration
@EnableJpaRepositories(basePackages = "com.ocean.shopping.repository")
@Profile("test")
public class TestDatabaseConfig {
    // Uses default Spring Boot JPA configuration for tests
}