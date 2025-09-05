package com.ocean.shopping.controller;

import com.ocean.shopping.service.lock.DistributedLock;
import com.ocean.shopping.service.lock.LockCleanupService;
import com.ocean.shopping.service.lock.LockMetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for monitoring and managing distributed locks.
 * Provides endpoints for administrators to view lock statistics and perform maintenance.
 */
@RestController
@RequestMapping("/api/admin/locks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Lock Monitoring", description = "Distributed lock monitoring and management")
@PreAuthorize("hasRole('ADMIN')")
public class LockMonitoringController {

    private final DistributedLock distributedLock;
    private final LockMetricsService lockMetricsService;
    private final LockCleanupService lockCleanupService;

    @GetMapping("/status")
    @Operation(summary = "Get lock system status", description = "Returns overall status of the distributed lock system")
    public ResponseEntity<Map<String, Object>> getLockSystemStatus() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            LockMetricsService.LockStatistics stats = lockMetricsService.getLockStatistics();
            LockCleanupService.CleanupStatus cleanupStatus = lockCleanupService.getCleanupStatus();
            
            status.put("system", "online");
            status.put("statistics", stats);
            status.put("cleanup", cleanupStatus);
            status.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Error getting lock system status", e);
            status.put("system", "error");
            status.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(status);
        }
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get detailed lock statistics", description = "Returns detailed performance metrics for the lock system")
    public ResponseEntity<LockMetricsService.LockStatistics> getLockStatistics() {
        try {
            return ResponseEntity.ok(lockMetricsService.getLockStatistics());
        } catch (Exception e) {
            log.error("Error getting lock statistics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/cleanup-status")
    @Operation(summary = "Get cleanup service status", description = "Returns status of the lock cleanup service")
    public ResponseEntity<LockCleanupService.CleanupStatus> getCleanupStatus() {
        try {
            return ResponseEntity.ok(lockCleanupService.getCleanupStatus());
        } catch (Exception e) {
            log.error("Error getting cleanup status", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/cleanup/force")
    @Operation(summary = "Force cleanup all locks", description = "Emergency operation to remove all locks - use with caution")
    public ResponseEntity<Map<String, Object>> forceCleanupAllLocks() {
        try {
            log.warn("Force cleanup requested - removing all locks");
            int cleanedCount = lockCleanupService.forceCleanupAllLocks();
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("cleanedCount", cleanedCount);
            result.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error during force cleanup", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @GetMapping("/check/{key}")
    @Operation(summary = "Check if specific key is locked", description = "Check if a specific lock key is currently locked")
    public ResponseEntity<Map<String, Object>> checkLockStatus(@PathVariable String key) {
        try {
            boolean isLocked = distributedLock.isLocked(key);
            String owner = distributedLock.getOwner(key);
            long remainingTtl = distributedLock.getRemainingTtl(key);
            
            Map<String, Object> result = new HashMap<>();
            result.put("key", key);
            result.put("locked", isLocked);
            result.put("owner", owner);
            result.put("remainingTtlSeconds", remainingTtl);
            result.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error checking lock status for key: {}", key, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/metrics/reset")
    @Operation(summary = "Reset lock metrics", description = "Reset all lock performance metrics - use for testing")
    public ResponseEntity<Map<String, Object>> resetMetrics() {
        try {
            lockMetricsService.resetMetrics();
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Lock metrics reset successfully");
            result.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error resetting metrics", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @GetMapping("/health")
    @Operation(summary = "Lock system health check", description = "Simple health check endpoint for load balancers")
    public ResponseEntity<Map<String, String>> healthCheck() {
        try {
            // Simple check - try to get statistics
            lockMetricsService.getLockStatistics();
            
            Map<String, String> health = new HashMap<>();
            health.put("status", "UP");
            health.put("system", "distributed-locks");
            
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            log.error("Lock system health check failed", e);
            Map<String, String> health = new HashMap<>();
            health.put("status", "DOWN");
            health.put("system", "distributed-locks");
            health.put("error", e.getMessage());
            
            return ResponseEntity.status(503).body(health);
        }
    }
}