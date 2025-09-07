package com.ocean.shopping.service.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Service responsible for cleaning up orphaned locks and preventing deadlocks.
 * Runs scheduled tasks to maintain Redis lock health.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LockCleanupService {

    private final RedisTemplate<String, String> stringRedisTemplate;
    private final LockMetricsService lockMetricsService;

    @Value("${app.lock.cleanup.orphaned-threshold-minutes:5}")
    private long orphanedThresholdMinutes;

    @Value("${app.lock.cleanup.max-locks-per-cleanup:100}")
    private int maxLocksPerCleanup;

    @Value("${app.lock.cleanup.enabled:true}")
    private boolean cleanupEnabled;

    // Lua script for safe orphaned lock cleanup
    private static final String CLEANUP_ORPHANED_SCRIPT = 
        "local keys = redis.call('keys', ARGV[1]) " +
        "local cleaned = 0 " +
        "for i=1,#keys do " +
        "    local ttl = redis.call('ttl', keys[i]) " +
        "    if ttl == -1 then " +  // Key exists but has no TTL (orphaned)
        "        redis.call('del', keys[i]) " +
        "        cleaned = cleaned + 1 " +
        "    end " +
        "end " +
        "return cleaned";

    private static final String LOCK_PATTERN = "lock:*";
    
    @PostConstruct
    public void initialize() {
        if (cleanupEnabled) {
            log.info("Lock cleanup service initialized - orphaned threshold: {}min, max per cleanup: {}", 
                    orphanedThresholdMinutes, maxLocksPerCleanup);
        } else {
            log.info("Lock cleanup service disabled");
        }
    }

    /**
     * Scheduled cleanup of orphaned locks (locks without TTL)
     * Runs every 2 minutes
     */
    @Scheduled(fixedRate = 120000) // 2 minutes
    public void cleanupOrphanedLocks() {
        if (!cleanupEnabled) {
            return;
        }

        try {
            long startTime = System.currentTimeMillis();
            
            // Execute cleanup script
            Long cleanedCount = stringRedisTemplate.execute(
                RedisScript.of(CLEANUP_ORPHANED_SCRIPT, Long.class),
                Collections.emptyList(),
                LOCK_PATTERN
            );

            long duration = System.currentTimeMillis() - startTime;
            
            if (cleanedCount != null && cleanedCount > 0) {
                log.info("Cleaned up {} orphaned locks in {}ms", cleanedCount, duration);
                
                // Record cleanup metrics
                for (int i = 0; i < cleanedCount; i++) {
                    lockMetricsService.recordLockRelease("orphaned:cleanup:" + i);
                }
            }
            
        } catch (Exception e) {
            log.error("Error during orphaned lock cleanup", e);
        }
    }

    /**
     * Cleanup expired locks that somehow still exist in Redis
     * Runs every 5 minutes
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void cleanupExpiredLocks() {
        if (!cleanupEnabled) {
            return;
        }

        try {
            Set<String> lockKeys = stringRedisTemplate.keys(LOCK_PATTERN);
            
            if (lockKeys == null || lockKeys.isEmpty()) {
                return;
            }

            int cleanedCount = 0;
            long startTime = System.currentTimeMillis();
            
            for (String key : lockKeys) {
                if (cleanedCount >= maxLocksPerCleanup) {
                    log.warn("Reached max locks per cleanup limit ({}), stopping cleanup", maxLocksPerCleanup);
                    break;
                }

                try {
                    Long ttl = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
                    
                    // If TTL is -2, the key has already expired and been removed
                    // If TTL is -1, the key exists but has no expiration (orphaned)
                    if (ttl != null && ttl == -1) {
                        // This is an orphaned lock - remove it
                        Boolean deleted = stringRedisTemplate.delete(key);
                        if (Boolean.TRUE.equals(deleted)) {
                            cleanedCount++;
                            log.debug("Removed orphaned lock: {}", key);
                        }
                    }
                    
                } catch (Exception e) {
                    log.warn("Error checking lock expiration for key: {}", key, e);
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            
            if (cleanedCount > 0) {
                log.info("Cleaned up {} expired locks from {} total locks in {}ms", 
                        cleanedCount, lockKeys.size(), duration);
            }
            
        } catch (Exception e) {
            log.error("Error during expired lock cleanup", e);
        }
    }

    /**
     * Health check for lock system
     * Runs every 10 minutes
     */
    @Scheduled(fixedRate = 600000) // 10 minutes
    public void performHealthCheck() {
        if (!cleanupEnabled) {
            return;
        }

        try {
            long actualLocks = lockMetricsService.getActualActiveLockCount();
            long trackedLocks = (long) lockMetricsService.getActiveLockCount();
            
            log.info("Lock health check - Redis: {} locks, Tracked: {} locks", actualLocks, trackedLocks);
            
            // Alert if there's a significant discrepancy
            if (actualLocks >= 0 && Math.abs(actualLocks - trackedLocks) > 10) {
                log.warn("Lock count discrepancy detected - Redis: {}, Tracked: {}", actualLocks, trackedLocks);
            }
            
            // Check for high lock contention
            LockMetricsService.LockStatistics stats = lockMetricsService.getLockStatistics();
            if (stats.getSuccessRate() < 80.0 && stats.getTotalAcquisitions() > 100) {
                log.warn("Low lock acquisition success rate: {}% over {} attempts", 
                        stats.getSuccessRate(), stats.getTotalAcquisitions());
            }
            
        } catch (Exception e) {
            log.error("Error during lock health check", e);
        }
    }

    /**
     * Force cleanup of all locks (use with caution)
     * This is primarily for testing or emergency situations
     */
    public int forceCleanupAllLocks() {
        if (!cleanupEnabled) {
            log.warn("Lock cleanup is disabled - cannot perform force cleanup");
            return 0;
        }

        try {
            Set<String> lockKeys = stringRedisTemplate.keys(LOCK_PATTERN);
            
            if (lockKeys == null || lockKeys.isEmpty()) {
                return 0;
            }

            Long deleted = stringRedisTemplate.delete(lockKeys);
            int deletedCount = deleted != null ? deleted.intValue() : 0;
            
            log.warn("Force cleanup removed {} locks", deletedCount);
            
            // Reset metrics since all locks were removed
            lockMetricsService.resetMetrics();
            
            return deletedCount;
            
        } catch (Exception e) {
            log.error("Error during force cleanup", e);
            return 0;
        }
    }

    /**
     * Get cleanup service status
     */
    public CleanupStatus getCleanupStatus() {
        return CleanupStatus.builder()
                .enabled(cleanupEnabled)
                .orphanedThresholdMinutes(orphanedThresholdMinutes)
                .maxLocksPerCleanup(maxLocksPerCleanup)
                .actualLockCount(lockMetricsService.getActualActiveLockCount())
                .trackedLockCount((long) lockMetricsService.getActiveLockCount())
                .build();
    }

    /**
     * Data class for cleanup status
     */
    @lombok.Builder
    @lombok.Data
    public static class CleanupStatus {
        private final boolean enabled;
        private final long orphanedThresholdMinutes;
        private final int maxLocksPerCleanup;
        private final long actualLockCount;
        private final long trackedLockCount;
        
        public boolean hasDiscrepancy() {
            return actualLockCount >= 0 && Math.abs(actualLockCount - trackedLockCount) > 5;
        }
    }
}