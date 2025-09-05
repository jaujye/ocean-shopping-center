package com.ocean.shopping.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Health check controller for monitoring application status
 */
@RestController
@RequestMapping("/health")
@Tag(name = "Health Check", description = "Application health monitoring endpoints")
@Slf4j
public class HealthController {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @GetMapping
    @Operation(summary = "Basic health check", description = "Returns basic application health status")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", ZonedDateTime.now());
        health.put("application", "Ocean Shopping Center");
        health.put("version", "1.0.0");
        
        return ResponseEntity.ok(health);
    }

    @GetMapping("/detailed")
    @Operation(summary = "Detailed health check", description = "Returns detailed health status including dependencies")
    public ResponseEntity<Map<String, Object>> detailedHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", ZonedDateTime.now());
        health.put("application", "Ocean Shopping Center");
        health.put("version", "1.0.0");

        // Check database connectivity
        Map<String, Object> database = new HashMap<>();
        try (Connection connection = dataSource.getConnection()) {
            database.put("status", "UP");
            database.put("database", connection.getMetaData().getDatabaseProductName());
            database.put("version", connection.getMetaData().getDatabaseProductVersion());
            log.debug("Database health check: UP");
        } catch (Exception e) {
            database.put("status", "DOWN");
            database.put("error", e.getMessage());
            log.warn("Database health check: DOWN - {}", e.getMessage());
        }
        health.put("database", database);

        // Check Redis connectivity
        Map<String, Object> redis = new HashMap<>();
        try {
            redisConnectionFactory.getConnection().ping();
            redis.put("status", "UP");
            log.debug("Redis health check: UP");
        } catch (Exception e) {
            redis.put("status", "DOWN");
            redis.put("error", e.getMessage());
            log.warn("Redis health check: DOWN - {}", e.getMessage());
        }
        health.put("redis", redis);

        return ResponseEntity.ok(health);
    }

    @GetMapping("/ready")
    @Operation(summary = "Readiness check", description = "Returns readiness status for container orchestration")
    public ResponseEntity<Map<String, Object>> ready() {
        Map<String, Object> readiness = new HashMap<>();
        boolean isReady = true;

        // Check if database is accessible
        try (Connection connection = dataSource.getConnection()) {
            // Database is ready
        } catch (Exception e) {
            isReady = false;
            log.warn("Readiness check failed - database not ready: {}", e.getMessage());
        }

        // Check if Redis is accessible
        try {
            redisConnectionFactory.getConnection().ping();
            // Redis is ready
        } catch (Exception e) {
            isReady = false;
            log.warn("Readiness check failed - Redis not ready: {}", e.getMessage());
        }

        readiness.put("status", isReady ? "READY" : "NOT_READY");
        readiness.put("timestamp", ZonedDateTime.now());

        return ResponseEntity.ok(readiness);
    }

    @GetMapping("/live")
    @Operation(summary = "Liveness check", description = "Returns liveness status for container orchestration")
    public ResponseEntity<Map<String, Object>> live() {
        Map<String, Object> liveness = new HashMap<>();
        liveness.put("status", "ALIVE");
        liveness.put("timestamp", ZonedDateTime.now());
        
        return ResponseEntity.ok(liveness);
    }
}