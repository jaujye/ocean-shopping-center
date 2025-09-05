package com.ocean.shopping.service.lock;

import com.ocean.shopping.service.lock.DistributedLockManager.LockAcquisitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for distributed lock system with Redis backend
 */
@SpringBootTest
@Testcontainers
@TestPropertySource(properties = {
    "spring.data.redis.host=localhost",
    "spring.data.redis.port=6379",
    "app.lock.default-ttl-seconds=5",
    "app.lock.max-retries=3"
})
class DistributedLockIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @Autowired
    private DistributedLock distributedLock;

    @Autowired
    private DistributedLockManager lockManager;

    @Autowired
    private LockMetricsService metricsService;

    @Autowired
    private LockCleanupService cleanupService;

    @Autowired
    private RedisTemplate<String, String> stringRedisTemplate;

    @BeforeEach
    void setUp() {
        // Clean up any existing locks
        cleanupService.forceCleanupAllLocks();
        metricsService.resetMetrics();
        
        // Update Redis host from container
        System.setProperty("spring.data.redis.host", redis.getHost());
        System.setProperty("spring.data.redis.port", redis.getMappedPort(6379).toString());
    }

    @Test
    void testBasicLockAcquisitionAndRelease() {
        String lockKey = "test:basic:1";
        
        // Acquire lock
        String token = distributedLock.acquire(lockKey);
        assertNotNull(token, "Should acquire lock successfully");
        assertTrue(distributedLock.isLocked(lockKey), "Lock should be active");
        assertEquals(token, distributedLock.getOwner(lockKey), "Owner should match token");
        
        // Release lock
        boolean released = distributedLock.release(lockKey, token);
        assertTrue(released, "Should release lock successfully");
        assertFalse(distributedLock.isLocked(lockKey), "Lock should be released");
    }

    @Test
    void testLockContention() throws InterruptedException {
        String lockKey = "test:contention:1";
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    String result = lockManager.executeWithLock(lockKey, () -> {
                        // Simulate some work
                        Thread.sleep(100);
                        successCount.incrementAndGet();
                        return "Thread " + threadId + " succeeded";
                    });
                    
                    if (result != null) {
                        System.out.println("Success: " + result);
                    } else {
                        failureCount.incrementAndGet();
                        System.out.println("Thread " + threadId + " failed to acquire lock");
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    System.out.println("Thread " + threadId + " error: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS), "All threads should complete");
        executor.shutdown();
        
        // Only one thread should succeed at a time, but with retries, more might succeed
        assertTrue(successCount.get() > 0, "At least one thread should succeed");
        assertTrue(successCount.get() + failureCount.get() == threadCount, 
                  "All threads should be accounted for");
        
        System.out.println("Success: " + successCount.get() + ", Failures: " + failureCount.get());
    }

    @Test
    void testLockTimeout() throws InterruptedException {
        String lockKey = "test:timeout:1";
        
        // Acquire lock with short TTL
        String token = distributedLock.acquire(lockKey, 1, TimeUnit.SECONDS);
        assertNotNull(token, "Should acquire lock");
        assertTrue(distributedLock.isLocked(lockKey), "Lock should be active");
        
        // Wait for TTL to expire
        Thread.sleep(1500);
        
        // Lock should have expired
        assertFalse(distributedLock.isLocked(lockKey), "Lock should have expired");
        
        // Should be able to acquire again
        String newToken = distributedLock.acquire(lockKey);
        assertNotNull(newToken, "Should acquire lock after expiration");
        assertNotEquals(token, newToken, "New token should be different");
    }

    @Test
    void testLockExtension() {
        String lockKey = "test:extension:1";
        
        // Acquire lock
        String token = distributedLock.acquire(lockKey, 2, TimeUnit.SECONDS);
        assertNotNull(token, "Should acquire lock");
        
        // Extend lock
        boolean extended = distributedLock.extend(lockKey, token, 5, TimeUnit.SECONDS);
        assertTrue(extended, "Should extend lock successfully");
        
        // Verify extended TTL
        long remainingTtl = distributedLock.getRemainingTtl(lockKey);
        assertTrue(remainingTtl > 2, "TTL should be extended");
        
        // Clean up
        distributedLock.release(lockKey, token);
    }

    @Test
    void testCartOperationWithLock() {
        String userId = "user123";
        AtomicInteger operationCount = new AtomicInteger(0);
        
        // Simulate concurrent cart operations
        String result = lockManager.executeCartOperation(userId, () -> {
            operationCount.incrementAndGet();
            Thread.sleep(100); // Simulate processing time
            return "Cart operation completed";
        });
        
        assertEquals("Cart operation completed", result);
        assertEquals(1, operationCount.get());
    }

    @Test
    void testInventoryOperationWithLock() {
        String productId = "product456";
        AtomicInteger inventoryUpdate = new AtomicInteger(100);
        
        // Simulate inventory deduction
        String result = lockManager.executeInventoryOperation(productId, () -> {
            int currentInventory = inventoryUpdate.get();
            if (currentInventory >= 5) {
                inventoryUpdate.addAndGet(-5);
                return "Inventory updated: " + inventoryUpdate.get();
            } else {
                throw new RuntimeException("Insufficient inventory");
            }
        });
        
        assertEquals("Inventory updated: 95", result);
    }

    @Test
    void testLockAcquisitionException() {
        String lockKey = "test:exception:1";
        
        // Acquire lock first
        String token1 = distributedLock.acquire(lockKey);
        assertNotNull(token1, "First acquisition should succeed");
        
        // Try to acquire with executeWithLockOrThrow - should throw exception
        assertThrows(LockAcquisitionException.class, () -> {
            lockManager.executeWithLockOrThrow(lockKey, () -> {
                return "This should not execute";
            });
        });
        
        // Clean up
        distributedLock.release(lockKey, token1);
    }

    @Test
    void testLockWithFallback() {
        String lockKey = "test:fallback:1";
        
        // Acquire lock first
        String token1 = distributedLock.acquire(lockKey);
        assertNotNull(token1, "First acquisition should succeed");
        
        // Try with fallback
        String result = lockManager.executeWithLockOrFallback(
            lockKey, 
            () -> "Primary task",
            () -> "Fallback task"
        );
        
        assertEquals("Fallback task", result);
        
        // Clean up
        distributedLock.release(lockKey, token1);
    }

    @Test
    void testMetricsCollection() {
        String lockKey = "test:metrics:1";
        
        // Reset metrics
        metricsService.resetMetrics();
        
        // Perform some lock operations
        String token = distributedLock.acquire(lockKey);
        assertNotNull(token);
        distributedLock.release(lockKey, token);
        
        // Check metrics
        LockMetricsService.LockStatistics stats = metricsService.getLockStatistics();
        assertTrue(stats.getTotalAcquisitions() > 0, "Should record acquisitions");
        assertTrue(stats.getTotalReleases() > 0, "Should record releases");
        assertEquals(100.0, stats.getSuccessRate(), 0.1, "Success rate should be 100%");
    }

    @Test
    void testLockCleanup() {
        // Create some locks
        String lockKey1 = "test:cleanup:1";
        String lockKey2 = "test:cleanup:2";
        
        distributedLock.acquire(lockKey1);
        distributedLock.acquire(lockKey2);
        
        // Verify locks exist
        assertTrue(distributedLock.isLocked(lockKey1));
        assertTrue(distributedLock.isLocked(lockKey2));
        
        // Force cleanup
        int cleanedCount = cleanupService.forceCleanupAllLocks();
        assertTrue(cleanedCount >= 2, "Should clean up at least 2 locks");
        
        // Verify locks are gone
        assertFalse(distributedLock.isLocked(lockKey1));
        assertFalse(distributedLock.isLocked(lockKey2));
    }

    @Test
    void testHighConcurrencyScenario() throws InterruptedException {
        String lockKey = "test:high-concurrency";
        int threadCount = 100;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready
                    
                    String result = lockManager.executeWithLock(lockKey, () -> {
                        // Very short critical section
                        Thread.sleep(1);
                        return "success";
                    }, 1, TimeUnit.SECONDS, 2);
                    
                    if (result != null) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    completeLatch.countDown();
                }
            });
        }

        // Start all threads simultaneously
        startLatch.countDown();
        
        // Wait for completion
        assertTrue(completeLatch.await(30, TimeUnit.SECONDS), "All threads should complete");
        executor.shutdown();
        
        System.out.println("High concurrency test - Success: " + successCount.get() + 
                          ", Failures: " + failureCount.get());
        
        // At least some operations should succeed
        assertTrue(successCount.get() > 0, "Some operations should succeed");
        assertEquals(threadCount, successCount.get() + failureCount.get(), 
                    "All operations should be accounted for");
    }

    @Test
    void testLockPerformance() {
        String lockKey = "test:performance";
        int iterations = 1000;
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < iterations; i++) {
            String token = distributedLock.acquire(lockKey);
            assertNotNull(token);
            distributedLock.release(lockKey, token);
        }
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        double avgTime = (double) totalTime / iterations;
        
        System.out.println("Lock performance: " + iterations + " operations in " + 
                          totalTime + "ms, avg: " + avgTime + "ms per operation");
        
        // Should be fast - less than 5ms per operation on average
        assertTrue(avgTime < 5.0, "Lock operations should be fast (< 5ms average)");
    }
}