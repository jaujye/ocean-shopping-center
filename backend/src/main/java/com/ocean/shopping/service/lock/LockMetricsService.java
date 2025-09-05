package com.ocean.shopping.service.lock;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for collecting and reporting distributed lock metrics.
 * Integrates with Spring Boot Actuator and Micrometer for monitoring.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LockMetricsService {

    private final MeterRegistry meterRegistry;
    private final RedisTemplate<String, String> stringRedisTemplate;

    // Metrics counters
    private final Counter lockAcquisitionCounter;
    private final Counter lockFailureCounter;
    private final Counter lockReleaseCounter;
    private final Counter lockExtensionCounter;
    private final Counter lockErrorCounter;
    private final Timer lockAcquisitionTimer;

    // Active locks tracking
    private final AtomicLong activeLocks = new AtomicLong(0);
    private final ConcurrentHashMap<String, Long> lockAcquisitionTimes = new ConcurrentHashMap<>();

    public LockMetricsService(MeterRegistry meterRegistry, RedisTemplate<String, String> stringRedisTemplate) {
        this.meterRegistry = meterRegistry;
        this.stringRedisTemplate = stringRedisTemplate;

        // Initialize counters
        this.lockAcquisitionCounter = Counter.builder("lock.acquisitions")
                .description("Number of successful lock acquisitions")
                .register(meterRegistry);

        this.lockFailureCounter = Counter.builder("lock.failures")
                .description("Number of failed lock acquisition attempts")
                .register(meterRegistry);

        this.lockReleaseCounter = Counter.builder("lock.releases")
                .description("Number of successful lock releases")
                .register(meterRegistry);

        this.lockExtensionCounter = Counter.builder("lock.extensions")
                .description("Number of successful lock extensions")
                .register(meterRegistry);

        this.lockErrorCounter = Counter.builder("lock.errors")
                .description("Number of lock operation errors")
                .register(meterRegistry);

        this.lockAcquisitionTimer = Timer.builder("lock.acquisition.time")
                .description("Time taken to acquire locks")
                .register(meterRegistry);

        // Register gauge for active locks
        Gauge.builder("lock.active.count")
                .description("Number of currently active locks")
                .register(meterRegistry, this, LockMetricsService::getActiveLockCount);
    }

    /**
     * Record successful lock acquisition
     */
    public void recordLockAcquisition(String key, long acquisitionTimeMs, int attemptCount) {
        lockAcquisitionCounter.increment();
        lockAcquisitionTimer.record(acquisitionTimeMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        
        activeLocks.incrementAndGet();
        lockAcquisitionTimes.put(key, System.currentTimeMillis());
        
        // Record acquisition attempts as a separate metric
        meterRegistry.counter("lock.acquisition.attempts", "attempts", String.valueOf(attemptCount))
                .increment();
        
        log.debug("Lock acquisition recorded: key={}, time={}ms, attempts={}", key, acquisitionTimeMs, attemptCount);
    }

    /**
     * Record failed lock acquisition
     */
    public void recordLockFailure(String key, int attemptCount) {
        lockFailureCounter.increment();
        
        meterRegistry.counter("lock.failure.attempts", "attempts", String.valueOf(attemptCount))
                .increment();
        
        log.debug("Lock failure recorded: key={}, attempts={}", key, attemptCount);
    }

    /**
     * Record successful lock release
     */
    public void recordLockRelease(String key) {
        lockReleaseCounter.increment();
        activeLocks.decrementAndGet();
        
        // Calculate hold duration if we have the acquisition time
        Long acquisitionTime = lockAcquisitionTimes.remove(key);
        if (acquisitionTime != null) {
            long holdDuration = System.currentTimeMillis() - acquisitionTime;
            meterRegistry.timer("lock.hold.duration")
                    .record(holdDuration, java.util.concurrent.TimeUnit.MILLISECONDS);
        }
        
        log.debug("Lock release recorded: key={}", key);
    }

    /**
     * Record successful lock extension
     */
    public void recordLockExtension(String key) {
        lockExtensionCounter.increment();
        log.debug("Lock extension recorded: key={}", key);
    }

    /**
     * Record lock operation error
     */
    public void recordLockError(String key, Exception error) {
        lockErrorCounter.increment();
        
        meterRegistry.counter("lock.errors", "error_type", error.getClass().getSimpleName())
                .increment();
        
        log.debug("Lock error recorded: key={}, error={}", key, error.getMessage());
    }

    /**
     * Get current active lock count
     */
    public double getActiveLockCount() {
        return activeLocks.get();
    }

    /**
     * Get lock statistics summary
     */
    public LockStatistics getLockStatistics() {
        return LockStatistics.builder()
                .totalAcquisitions((long) lockAcquisitionCounter.count())
                .totalFailures((long) lockFailureCounter.count())
                .totalReleases((long) lockReleaseCounter.count())
                .totalExtensions((long) lockExtensionCounter.count())
                .totalErrors((long) lockErrorCounter.count())
                .activeLocks(activeLocks.get())
                .averageAcquisitionTime(lockAcquisitionTimer.mean(java.util.concurrent.TimeUnit.MILLISECONDS))
                .build();
    }

    /**
     * Reset all metrics (useful for testing)
     */
    public void resetMetrics() {
        activeLocks.set(0);
        lockAcquisitionTimes.clear();
        log.info("Lock metrics reset");
    }

    /**
     * Get real-time active lock count from Redis
     */
    public long getActualActiveLockCount() {
        try {
            return stringRedisTemplate.keys("lock:*").size();
        } catch (Exception e) {
            log.error("Failed to get actual active lock count from Redis", e);
            return -1;
        }
    }

    /**
     * Data class for lock statistics
     */
    @lombok.Builder
    @lombok.Data
    public static class LockStatistics {
        private final long totalAcquisitions;
        private final long totalFailures;
        private final long totalReleases;
        private final long totalExtensions;
        private final long totalErrors;
        private final long activeLocks;
        private final double averageAcquisitionTime;
        
        public double getSuccessRate() {
            long total = totalAcquisitions + totalFailures;
            return total > 0 ? (double) totalAcquisitions / total * 100.0 : 0.0;
        }
    }
}