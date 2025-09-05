package com.ocean.shopping.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration for distributed lock system
 * Enables scheduling for lock cleanup tasks
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.lock.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class LockConfiguration {

    public LockConfiguration() {
        log.info("Distributed lock system configuration loaded");
    }
}