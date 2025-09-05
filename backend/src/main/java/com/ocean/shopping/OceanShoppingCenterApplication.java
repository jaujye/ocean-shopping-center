package com.ocean.shopping;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Main Spring Boot application class for Ocean Shopping Center
 * Multi-tenant e-commerce platform backend
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableAsync
@EnableTransactionManagement
public class OceanShoppingCenterApplication {

    public static void main(String[] args) {
        SpringApplication.run(OceanShoppingCenterApplication.class, args);
    }
}