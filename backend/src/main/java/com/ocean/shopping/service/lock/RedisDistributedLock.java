package com.ocean.shopping.service.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Redis-based distributed lock implementation with deadlock prevention
 * and automatic cleanup mechanisms.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisDistributedLock implements DistributedLock {

    private final RedisTemplate<String, String> stringRedisTemplate;
    private final LockMetricsService lockMetricsService;

    // Configuration properties
    @Value("${app.lock.default-ttl-seconds:30}")
    private long defaultTtlSeconds;

    @Value("${app.lock.max-retries:5}")
    private int defaultMaxRetries;

    @Value("${app.lock.retry-base-delay-ms:100}")
    private long retryBaseDelayMs;

    @Value("${app.lock.retry-max-delay-ms:2000}")
    private long retryMaxDelayMs;

    // Lock key prefix
    private static final String LOCK_PREFIX = "lock:";
    
    // Lua script for atomic lock acquisition
    private static final String ACQUIRE_SCRIPT = 
        "if redis.call('exists', KEYS[1]) == 0 then " +
        "    return redis.call('setex', KEYS[1], ARGV[2], ARGV[1]) " +
        "else " +
        "    return nil " +
        "end";

    // Lua script for atomic lock release
    private static final String RELEASE_SCRIPT = 
        "if redis.call('get', KEYS[1]) == ARGV[1] then " +
        "    return redis.call('del', KEYS[1]) " +
        "else " +
        "    return 0 " +
        "end";

    // Lua script for atomic lock extension
    private static final String EXTEND_SCRIPT = 
        "if redis.call('get', KEYS[1]) == ARGV[1] then " +
        "    return redis.call('expire', KEYS[1], ARGV[2]) " +
        "else " +
        "    return 0 " +
        "end";

    @Override
    public String acquire(String key) {
        return acquire(key, defaultTtlSeconds, TimeUnit.SECONDS, defaultMaxRetries);
    }

    @Override
    public String acquire(String key, long ttl, TimeUnit timeUnit) {
        return acquire(key, ttl, timeUnit, defaultMaxRetries);
    }

    @Override
    public String acquire(String key, long ttl, TimeUnit timeUnit, int maxRetries) {
        validateLockKey(key);
        
        String lockKey = LOCK_PREFIX + key;
        String token = generateToken();
        long ttlSeconds = timeUnit.toSeconds(ttl);
        
        log.debug("Attempting to acquire lock: {} with TTL: {}s, maxRetries: {}", lockKey, ttlSeconds, maxRetries);
        
        long startTime = System.currentTimeMillis();
        
        try {
            for (int attempt = 0; attempt <= maxRetries; attempt++) {
                // Try to acquire the lock
                String result = stringRedisTemplate.execute(
                    RedisScript.of(ACQUIRE_SCRIPT, String.class),
                    Collections.singletonList(lockKey),
                    token,
                    String.valueOf(ttlSeconds)
                );
                
                if ("OK".equals(result)) {
                    long acquisitionTime = System.currentTimeMillis() - startTime;
                    lockMetricsService.recordLockAcquisition(key, acquisitionTime, attempt);
                    log.debug("Lock acquired successfully: {} with token: {} after {} attempts", lockKey, token, attempt + 1);
                    return token;
                }
                
                // If not the last attempt, wait before retrying
                if (attempt < maxRetries) {
                    long delay = calculateRetryDelay(attempt);
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.warn("Lock acquisition interrupted for key: {}", lockKey);
                        break;
                    }
                }
            }
            
            // Failed to acquire lock
            lockMetricsService.recordLockFailure(key, maxRetries + 1);
            log.warn("Failed to acquire lock: {} after {} attempts", lockKey, maxRetries + 1);
            return null;
            
        } catch (Exception e) {
            lockMetricsService.recordLockError(key, e);
            log.error("Error acquiring lock: {}", lockKey, e);
            return null;
        }
    }

    @Override
    public boolean release(String key, String token) {
        if (token == null) {
            log.warn("Cannot release lock with null token for key: {}", key);
            return false;
        }
        
        validateLockKey(key);
        String lockKey = LOCK_PREFIX + key;
        
        log.debug("Attempting to release lock: {} with token: {}", lockKey, token);
        
        try {
            Long result = stringRedisTemplate.execute(
                RedisScript.of(RELEASE_SCRIPT, Long.class),
                Collections.singletonList(lockKey),
                token
            );
            
            boolean released = result != null && result == 1;
            if (released) {
                lockMetricsService.recordLockRelease(key);
                log.debug("Lock released successfully: {}", lockKey);
            } else {
                log.warn("Failed to release lock: {} - token mismatch or lock expired", lockKey);
            }
            
            return released;
            
        } catch (Exception e) {
            lockMetricsService.recordLockError(key, e);
            log.error("Error releasing lock: {}", lockKey, e);
            return false;
        }
    }

    @Override
    public boolean extend(String key, String token, long ttl, TimeUnit timeUnit) {
        if (token == null) {
            log.warn("Cannot extend lock with null token for key: {}", key);
            return false;
        }
        
        validateLockKey(key);
        String lockKey = LOCK_PREFIX + key;
        long ttlSeconds = timeUnit.toSeconds(ttl);
        
        log.debug("Attempting to extend lock: {} with token: {} for {}s", lockKey, token, ttlSeconds);
        
        try {
            Long result = stringRedisTemplate.execute(
                RedisScript.of(EXTEND_SCRIPT, Long.class),
                Collections.singletonList(lockKey),
                token,
                String.valueOf(ttlSeconds)
            );
            
            boolean extended = result != null && result == 1;
            if (extended) {
                lockMetricsService.recordLockExtension(key);
                log.debug("Lock extended successfully: {}", lockKey);
            } else {
                log.warn("Failed to extend lock: {} - token mismatch or lock expired", lockKey);
            }
            
            return extended;
            
        } catch (Exception e) {
            lockMetricsService.recordLockError(key, e);
            log.error("Error extending lock: {}", lockKey, e);
            return false;
        }
    }

    @Override
    public boolean isLocked(String key) {
        validateLockKey(key);
        String lockKey = LOCK_PREFIX + key;
        
        try {
            return Boolean.TRUE.equals(stringRedisTemplate.hasKey(lockKey));
        } catch (Exception e) {
            log.error("Error checking lock existence: {}", lockKey, e);
            return false;
        }
    }

    @Override
    public String getOwner(String key) {
        validateLockKey(key);
        String lockKey = LOCK_PREFIX + key;
        
        try {
            return stringRedisTemplate.opsForValue().get(lockKey);
        } catch (Exception e) {
            log.error("Error getting lock owner: {}", lockKey, e);
            return null;
        }
    }

    @Override
    public long getRemainingTtl(String key) {
        validateLockKey(key);
        String lockKey = LOCK_PREFIX + key;
        
        try {
            Long ttl = stringRedisTemplate.getExpire(lockKey, TimeUnit.SECONDS);
            return ttl != null ? ttl : -1;
        } catch (Exception e) {
            log.error("Error getting lock TTL: {}", lockKey, e);
            return -1;
        }
    }

    /**
     * Generate a unique token for the lock
     */
    private String generateToken() {
        return UUID.randomUUID().toString() + ":" + Instant.now().toEpochMilli();
    }

    /**
     * Calculate retry delay using exponential backoff with jitter
     */
    private long calculateRetryDelay(int attemptNumber) {
        // Exponential backoff: baseDelay * 2^attemptNumber
        long delay = retryBaseDelayMs * (1L << attemptNumber);
        
        // Cap the delay at maximum
        delay = Math.min(delay, retryMaxDelayMs);
        
        // Add jitter (0-25% of delay)
        long jitter = ThreadLocalRandom.current().nextLong(0, delay / 4 + 1);
        
        return delay + jitter;
    }

    /**
     * Validate lock key format
     */
    private void validateLockKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Lock key cannot be null or empty");
        }
        
        if (key.length() > 250) {
            throw new IllegalArgumentException("Lock key too long (max 250 characters)");
        }
        
        // Recommended key pattern: {service}:{resource}:{id}
        if (!key.matches("^[a-zA-Z0-9_-]+:[a-zA-Z0-9_-]+:[a-zA-Z0-9_-]+$")) {
            log.warn("Lock key '{}' does not follow recommended pattern 'service:resource:id'", key);
        }
    }
}