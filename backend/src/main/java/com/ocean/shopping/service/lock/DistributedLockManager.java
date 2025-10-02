package com.ocean.shopping.service.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * High-level distributed lock manager that provides convenient methods
 * for common locking patterns in the Ocean Shopping Center application.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DistributedLockManager {

    private final DistributedLock distributedLock;

    /**
     * Execute code with a distributed lock, automatically releasing the lock afterwards
     * @param lockKey The lock key
     * @param task The task to execute
     * @param <T> Return type
     * @return Task result or null if lock could not be acquired
     */
    public <T> T executeWithLock(String lockKey, Callable<T> task) {
        return executeWithLock(lockKey, task, 30, TimeUnit.SECONDS, 5);
    }

    /**
     * Execute code with a distributed lock with custom timeout and retries
     * @param lockKey The lock key
     * @param task The task to execute
     * @param ttl Lock timeout
     * @param timeUnit Time unit for timeout
     * @param maxRetries Maximum retry attempts
     * @param <T> Return type
     * @return Task result or null if lock could not be acquired
     */
    public <T> T executeWithLock(String lockKey, Callable<T> task, long ttl, TimeUnit timeUnit, int maxRetries) {
        String token = distributedLock.acquire(lockKey, ttl, timeUnit, maxRetries);
        
        if (token == null) {
            log.warn("Could not acquire lock for key: {}", lockKey);
            return null;
        }

        try {
            log.debug("Executing task with lock: {}", lockKey);
            return task.call();
        } catch (Exception e) {
            log.error("Error executing task with lock: {}", lockKey, e);
            throw new RuntimeException("Error executing locked task", e);
        } finally {
            boolean released = distributedLock.release(lockKey, token);
            if (!released) {
                log.warn("Failed to release lock: {} with token: {}", lockKey, token);
            }
        }
    }

    /**
     * Execute code with a distributed lock, throwing exception if lock cannot be acquired
     * @param lockKey The lock key
     * @param task The task to execute
     * @param <T> Return type
     * @return Task result
     * @throws LockAcquisitionException if lock cannot be acquired
     */
    public <T> T executeWithLockOrThrow(String lockKey, Callable<T> task) throws LockAcquisitionException {
        T result = executeWithLock(lockKey, task);
        if (result == null) {
            throw new LockAcquisitionException("Could not acquire lock for key: " + lockKey);
        }
        return result;
    }

    /**
     * Try to execute with a lock, return default value if lock cannot be acquired
     * @param lockKey The lock key
     * @param task The task to execute
     * @param defaultValue Default value if lock cannot be acquired
     * @param <T> Return type
     * @return Task result or default value
     */
    public <T> T executeWithLockOrDefault(String lockKey, Callable<T> task, T defaultValue) {
        T result = executeWithLock(lockKey, task);
        return result != null ? result : defaultValue;
    }

    /**
     * Execute with lock or use fallback supplier
     * @param lockKey The lock key
     * @param primaryTask Primary task to execute with lock
     * @param fallbackTask Fallback task if lock cannot be acquired
     * @param <T> Return type
     * @return Result from primary or fallback task
     */
    public <T> T executeWithLockOrFallback(String lockKey, Callable<T> primaryTask, Supplier<T> fallbackTask) {
        T result = executeWithLock(lockKey, primaryTask);
        if (result == null) {
            log.debug("Using fallback for lock key: {}", lockKey);
            return fallbackTask.get();
        }
        return result;
    }

    /**
     * Execute code with multiple distributed locks acquired in sorted order to prevent deadlocks.
     * Locks are sorted alphabetically before acquisition to ensure consistent lock ordering.
     *
     * @param lockKeys Collection of lock keys to acquire
     * @param task The task to execute with all locks held
     * @param <T> Return type
     * @return Task result
     * @throws LockAcquisitionException if any lock cannot be acquired
     */
    public <T> T executeWithMultipleLocks(Collection<String> lockKeys, Callable<T> task) throws LockAcquisitionException {
        if (lockKeys == null || lockKeys.isEmpty()) {
            try {
                return task.call();
            } catch (Exception e) {
                throw new RuntimeException("Error executing task without locks", e);
            }
        }

        // Sort lock keys to ensure consistent acquisition order and prevent deadlocks
        List<String> sortedKeys = new ArrayList<>(lockKeys);
        Collections.sort(sortedKeys);

        log.debug("Acquiring {} locks in order: {}", sortedKeys.size(), sortedKeys);
        return acquireLocksRecursively(sortedKeys, 0, new ArrayList<>(), task);
    }

    /**
     * Recursively acquire locks in order
     */
    private <T> T acquireLocksRecursively(List<String> lockKeys, int index, List<String> acquiredTokens, Callable<T> task) {
        if (index >= lockKeys.size()) {
            // All locks acquired, execute task
            try {
                return task.call();
            } catch (Exception e) {
                throw new RuntimeException("Error executing task with locks", e);
            } finally {
                // Release all locks in reverse order
                for (int i = acquiredTokens.size() - 1; i >= 0; i--) {
                    distributedLock.release(lockKeys.get(i), acquiredTokens.get(i));
                }
            }
        }

        // Acquire next lock
        String lockKey = lockKeys.get(index);
        String token = distributedLock.acquire(lockKey, 30, TimeUnit.SECONDS, 5);

        if (token == null) {
            // Failed to acquire lock, release all previously acquired locks
            for (int i = acquiredTokens.size() - 1; i >= 0; i--) {
                distributedLock.release(lockKeys.get(i), acquiredTokens.get(i));
            }
            throw new LockAcquisitionException("Could not acquire lock for key: " + lockKey);
        }

        acquiredTokens.add(token);

        // Continue acquiring remaining locks
        return acquireLocksRecursively(lockKeys, index + 1, acquiredTokens, task);
    }

    // Convenience methods for common lock key patterns

    /**
     * Create lock key for cart operations
     */
    public String cartLockKey(String userId) {
        return String.format("cart:user:%s", userId);
    }

    /**
     * Create lock key for inventory operations
     */
    public String inventoryLockKey(String productId) {
        return String.format("inventory:product:%s", productId);
    }

    /**
     * Create lock key for order operations
     */
    public String orderLockKey(String orderId) {
        return String.format("order:process:%s", orderId);
    }

    /**
     * Create lock key for payment operations
     */
    public String paymentLockKey(String paymentId) {
        return String.format("payment:process:%s", paymentId);
    }

    /**
     * Create lock key for user operations
     */
    public String userLockKey(String userId) {
        return String.format("user:update:%s", userId);
    }

    /**
     * Create lock key for coupon operations
     */
    public String couponLockKey(String couponCode) {
        return String.format("coupon:redeem:%s", couponCode);
    }

    /**
     * Execute cart operation with appropriate lock
     */
    public <T> T executeCartOperation(String userId, Callable<T> operation) {
        return executeWithLock(cartLockKey(userId), operation, 10, TimeUnit.SECONDS, 3);
    }

    /**
     * Execute inventory operation with appropriate lock
     */
    public <T> T executeInventoryOperation(String productId, Callable<T> operation) {
        return executeWithLock(inventoryLockKey(productId), operation, 5, TimeUnit.SECONDS, 5);
    }

    /**
     * Execute order operation with appropriate lock
     */
    public <T> T executeOrderOperation(String orderId, Callable<T> operation) {
        return executeWithLock(orderLockKey(orderId), operation, 60, TimeUnit.SECONDS, 3);
    }

    /**
     * Execute payment operation with appropriate lock
     */
    public <T> T executePaymentOperation(String paymentId, Callable<T> operation) {
        return executeWithLock(paymentLockKey(paymentId), operation, 30, TimeUnit.SECONDS, 2);
    }

    /**
     * Execute coupon operation with appropriate lock
     */
    public <T> T executeCouponOperation(String couponCode, Callable<T> operation) {
        return executeWithLock(couponLockKey(couponCode), operation, 15, TimeUnit.SECONDS, 3);
    }

    /**
     * Check if any critical resource is locked
     * @param userId User ID to check
     * @param productIds Product IDs to check
     * @return true if any resource is locked
     */
    public boolean hasActiveLocks(String userId, String... productIds) {
        if (distributedLock.isLocked(cartLockKey(userId))) {
            return true;
        }
        
        for (String productId : productIds) {
            if (distributedLock.isLocked(inventoryLockKey(productId))) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Custom exception for lock acquisition failures
     */
    public static class LockAcquisitionException extends RuntimeException {
        public LockAcquisitionException(String message) {
            super(message);
        }
        
        public LockAcquisitionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}