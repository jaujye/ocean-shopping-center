package com.ocean.shopping.service.lock;

import java.util.concurrent.TimeUnit;

/**
 * Distributed lock interface for preventing race conditions in distributed systems.
 * Uses Redis as the backend lock manager with built-in timeout and retry mechanisms.
 */
public interface DistributedLock {

    /**
     * Acquire a distributed lock with default TTL and retries
     * @param key The lock key (should follow pattern: lock:{service}:{resource}:{id})
     * @return Lock token if successful, null if failed to acquire
     */
    String acquire(String key);

    /**
     * Acquire a distributed lock with custom TTL
     * @param key The lock key
     * @param ttl Time to live for the lock
     * @param timeUnit Time unit for TTL
     * @return Lock token if successful, null if failed to acquire
     */
    String acquire(String key, long ttl, TimeUnit timeUnit);

    /**
     * Acquire a distributed lock with custom TTL and retry count
     * @param key The lock key
     * @param ttl Time to live for the lock
     * @param timeUnit Time unit for TTL
     * @param maxRetries Maximum number of retry attempts
     * @return Lock token if successful, null if failed to acquire
     */
    String acquire(String key, long ttl, TimeUnit timeUnit, int maxRetries);

    /**
     * Release a distributed lock
     * @param key The lock key
     * @param token The lock token returned from acquire()
     * @return true if successfully released, false otherwise
     */
    boolean release(String key, String token);

    /**
     * Extend the TTL of an existing lock
     * @param key The lock key
     * @param token The lock token
     * @param ttl New time to live
     * @param timeUnit Time unit for TTL
     * @return true if successfully extended, false otherwise
     */
    boolean extend(String key, String token, long ttl, TimeUnit timeUnit);

    /**
     * Check if a key is currently locked
     * @param key The lock key
     * @return true if locked, false otherwise
     */
    boolean isLocked(String key);

    /**
     * Get the owner token of a lock
     * @param key The lock key
     * @return Owner token if lock exists, null otherwise
     */
    String getOwner(String key);

    /**
     * Get the remaining TTL of a lock
     * @param key The lock key
     * @return Remaining TTL in seconds, -1 if lock doesn't exist, -2 if no TTL
     */
    long getRemainingTtl(String key);
}