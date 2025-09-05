package com.ocean.shopping.service.lock;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Load testing for distributed lock system to verify performance requirements
 * Enabled only when ENABLE_LOAD_TESTS environment variable is set
 */
@SpringBootTest
@TestPropertySource(properties = {
    "app.lock.default-ttl-seconds=5",
    "app.lock.max-retries=3",
    "app.lock.retry-base-delay-ms=50"
})
@EnabledIfEnvironmentVariable(named = "ENABLE_LOAD_TESTS", matches = "true")
@Slf4j
class DistributedLockLoadTest {

    @Autowired
    private DistributedLock distributedLock;

    @Autowired
    private DistributedLockManager lockManager;

    @Autowired
    private LockMetricsService metricsService;

    @BeforeEach
    void setUp() {
        metricsService.resetMetrics();
    }

    @Test
    void testHighVolumeCartOperations() throws InterruptedException {
        int userCount = 100;
        int operationsPerUser = 10;
        int totalOperations = userCount * operationsPerUser;
        
        log.info("Starting high volume cart operations test: {} users, {} operations each", 
                userCount, operationsPerUser);

        ExecutorService executor = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(totalOperations);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicLong totalResponseTime = new AtomicLong(0);

        long testStartTime = System.currentTimeMillis();

        for (int userId = 1; userId <= userCount; userId++) {
            final String userIdStr = "user" + userId;
            
            for (int op = 1; op <= operationsPerUser; op++) {
                executor.submit(() -> {
                    long startTime = System.nanoTime();
                    try {
                        String result = lockManager.executeCartOperation(userIdStr, () -> {
                            // Simulate cart operation (add/update/remove)
                            Thread.sleep(ThreadLocalRandom.current().nextInt(1, 10));
                            return "cart_operation_success";
                        });
                        
                        long endTime = System.nanoTime();
                        totalResponseTime.addAndGet(endTime - startTime);
                        
                        if (result != null) {
                            successCount.incrementAndGet();
                        } else {
                            failureCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                        log.warn("Cart operation failed: {}", e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                });
            }
        }

        assertTrue(latch.await(120, TimeUnit.SECONDS), "Load test should complete within 2 minutes");
        executor.shutdown();

        long testDuration = System.currentTimeMillis() - testStartTime;
        double avgResponseTimeMs = totalResponseTime.get() / 1_000_000.0 / totalOperations;
        double successRate = (double) successCount.get() / totalOperations * 100;
        double throughput = (double) totalOperations / testDuration * 1000; // operations per second

        log.info("High volume cart operations test results:");
        log.info("  Total operations: {}", totalOperations);
        log.info("  Success count: {}", successCount.get());
        log.info("  Failure count: {}", failureCount.get());
        log.info("  Success rate: {:.2f}%", successRate);
        log.info("  Average response time: {:.2f}ms", avgResponseTimeMs);
        log.info("  Throughput: {:.2f} ops/sec", throughput);
        log.info("  Test duration: {}ms", testDuration);

        // Performance assertions
        assertTrue(successRate >= 95.0, "Success rate should be at least 95%");
        assertTrue(avgResponseTimeMs < 50.0, "Average response time should be less than 50ms");
        assertTrue(throughput > 100.0, "Throughput should be more than 100 ops/sec");
    }

    @Test
    void testInventoryContentionScenario() throws InterruptedException {
        int productCount = 10;
        int concurrentBuyers = 50;
        int attemptsPerBuyer = 5;
        
        log.info("Starting inventory contention test: {} products, {} buyers, {} attempts each",
                productCount, concurrentBuyers, attemptsPerBuyer);

        ExecutorService executor = Executors.newFixedThreadPool(concurrentBuyers);
        CountDownLatch latch = new CountDownLatch(concurrentBuyers * attemptsPerBuyer);
        AtomicInteger totalAttempts = new AtomicInteger(0);
        AtomicInteger successfulPurchases = new AtomicInteger(0);
        AtomicInteger failedPurchases = new AtomicInteger(0);
        AtomicLong totalLockTime = new AtomicLong(0);

        // Simulate inventory for each product
        ConcurrentHashMap<String, AtomicInteger> inventory = new ConcurrentHashMap<>();
        for (int i = 1; i <= productCount; i++) {
            inventory.put("product" + i, new AtomicInteger(10)); // 10 units per product
        }

        long testStartTime = System.currentTimeMillis();

        for (int buyer = 1; buyer <= concurrentBuyers; buyer++) {
            final int buyerId = buyer;
            
            for (int attempt = 1; attempt <= attemptsPerBuyer; attempt++) {
                executor.submit(() -> {
                    try {
                        // Random product selection
                        String productId = "product" + ThreadLocalRandom.current().nextInt(1, productCount + 1);
                        int requestedQuantity = ThreadLocalRandom.current().nextInt(1, 4);
                        
                        long lockStartTime = System.nanoTime();
                        
                        String result = lockManager.executeInventoryOperation(productId, () -> {
                            AtomicInteger stock = inventory.get(productId);
                            int currentStock = stock.get();
                            
                            if (currentStock >= requestedQuantity) {
                                // Simulate processing time
                                Thread.sleep(ThreadLocalRandom.current().nextInt(5, 20));
                                stock.addAndGet(-requestedQuantity);
                                return "purchase_successful";
                            } else {
                                return "insufficient_inventory";
                            }
                        });
                        
                        long lockEndTime = System.nanoTime();
                        totalLockTime.addAndGet(lockEndTime - lockStartTime);
                        totalAttempts.incrementAndGet();
                        
                        if ("purchase_successful".equals(result)) {
                            successfulPurchases.incrementAndGet();
                        } else {
                            failedPurchases.incrementAndGet();
                        }
                        
                    } catch (Exception e) {
                        failedPurchases.incrementAndGet();
                        log.warn("Buyer {} failed: {}", buyerId, e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                });
            }
        }

        assertTrue(latch.await(180, TimeUnit.SECONDS), "Inventory contention test should complete");
        executor.shutdown();

        long testDuration = System.currentTimeMillis() - testStartTime;
        double avgLockTimeMs = totalLockTime.get() / 1_000_000.0 / totalAttempts.get();
        
        // Check final inventory
        int remainingInventory = inventory.values().stream()
                .mapToInt(AtomicInteger::get)
                .sum();

        log.info("Inventory contention test results:");
        log.info("  Total attempts: {}", totalAttempts.get());
        log.info("  Successful purchases: {}", successfulPurchases.get());
        log.info("  Failed purchases (insufficient inventory): {}", failedPurchases.get());
        log.info("  Remaining inventory: {}", remainingInventory);
        log.info("  Average lock time: {:.2f}ms", avgLockTimeMs);
        log.info("  Test duration: {}ms", testDuration);

        // Verify inventory consistency
        int initialInventory = productCount * 10;
        int soldItems = successfulPurchases.get(); // Assuming 1 item per successful purchase on average
        assertTrue(remainingInventory >= 0, "Inventory should never go negative");
        assertTrue(remainingInventory + soldItems <= initialInventory, "Inventory consistency check");
        assertTrue(avgLockTimeMs < 10.0, "Average lock time should be less than 10ms");
    }

    @Test
    void testMixedWorkloadStressTest() throws InterruptedException {
        int duration = 30; // seconds
        int threadCount = 100;
        
        log.info("Starting mixed workload stress test: {} threads for {} seconds", threadCount, duration);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        volatile boolean running = true;
        
        AtomicInteger cartOps = new AtomicInteger(0);
        AtomicInteger inventoryOps = new AtomicInteger(0);
        AtomicInteger orderOps = new AtomicInteger(0);
        AtomicInteger paymentOps = new AtomicInteger(0);
        AtomicInteger totalOps = new AtomicInteger(0);
        AtomicInteger errors = new AtomicInteger(0);
        AtomicLong totalResponseTime = new AtomicLong(0);

        // Start workers
        for (int i = 0; i < threadCount; i++) {
            final int workerId = i;
            executor.submit(() -> {
                while (running) {
                    try {
                        long startTime = System.nanoTime();
                        
                        // Random operation type
                        int opType = ThreadLocalRandom.current().nextInt(4);
                        String lockKey = "test:mixed:" + ThreadLocalRandom.current().nextInt(1000);
                        
                        switch (opType) {
                            case 0: // Cart operation
                                lockManager.executeCartOperation("user" + workerId, () -> {
                                    Thread.sleep(ThreadLocalRandom.current().nextInt(1, 5));
                                    cartOps.incrementAndGet();
                                    return "cart_op";
                                });
                                break;
                                
                            case 1: // Inventory operation
                                lockManager.executeInventoryOperation("product" + workerId, () -> {
                                    Thread.sleep(ThreadLocalRandom.current().nextInt(1, 5));
                                    inventoryOps.incrementAndGet();
                                    return "inventory_op";
                                });
                                break;
                                
                            case 2: // Order operation
                                lockManager.executeOrderOperation("order" + workerId, () -> {
                                    Thread.sleep(ThreadLocalRandom.current().nextInt(5, 15));
                                    orderOps.incrementAndGet();
                                    return "order_op";
                                });
                                break;
                                
                            case 3: // Payment operation
                                lockManager.executePaymentOperation("payment" + workerId, () -> {
                                    Thread.sleep(ThreadLocalRandom.current().nextInt(3, 10));
                                    paymentOps.incrementAndGet();
                                    return "payment_op";
                                });
                                break;
                        }
                        
                        long endTime = System.nanoTime();
                        totalResponseTime.addAndGet(endTime - startTime);
                        totalOps.incrementAndGet();
                        
                    } catch (Exception e) {
                        errors.incrementAndGet();
                        if (errors.get() % 100 == 0) {
                            log.warn("Error count reached: {}", errors.get());
                        }
                    }
                }
            });
        }

        // Let it run for specified duration
        Thread.sleep(duration * 1000);
        running = false;
        
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS), "Threads should shutdown gracefully");

        double avgResponseTimeMs = totalResponseTime.get() / 1_000_000.0 / totalOps.get();
        double errorRate = (double) errors.get() / totalOps.get() * 100;
        double throughput = (double) totalOps.get() / duration;

        log.info("Mixed workload stress test results:");
        log.info("  Duration: {} seconds", duration);
        log.info("  Total operations: {}", totalOps.get());
        log.info("  Cart operations: {}", cartOps.get());
        log.info("  Inventory operations: {}", inventoryOps.get());
        log.info("  Order operations: {}", orderOps.get());
        log.info("  Payment operations: {}", paymentOps.get());
        log.info("  Errors: {}", errors.get());
        log.info("  Error rate: {:.2f}%", errorRate);
        log.info("  Average response time: {:.2f}ms", avgResponseTimeMs);
        log.info("  Throughput: {:.2f} ops/sec", throughput);

        // Performance assertions
        assertTrue(errorRate < 5.0, "Error rate should be less than 5%");
        assertTrue(avgResponseTimeMs < 20.0, "Average response time should be less than 20ms");
        assertTrue(throughput > 500.0, "Throughput should be more than 500 ops/sec");
        assertTrue(totalOps.get() > 1000, "Should complete at least 1000 operations");
    }

    @Test
    void testLockMetricsUnderLoad() throws InterruptedException {
        int operationCount = 5000;
        int concurrency = 50;
        
        log.info("Testing lock metrics under load: {} operations with {} concurrent threads", 
                operationCount, concurrency);

        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        CountDownLatch latch = new CountDownLatch(operationCount);
        
        long testStartTime = System.currentTimeMillis();

        for (int i = 0; i < operationCount; i++) {
            final int opId = i;
            executor.submit(() -> {
                try {
                    String lockKey = "metrics:test:" + (opId % 100); // Create contention
                    
                    String token = distributedLock.acquire(lockKey, 1, TimeUnit.SECONDS, 2);
                    if (token != null) {
                        Thread.sleep(1); // Minimal critical section
                        distributedLock.release(lockKey, token);
                    }
                    
                } catch (Exception e) {
                    log.warn("Operation {} failed: {}", opId, e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(60, TimeUnit.SECONDS), "Metrics test should complete");
        executor.shutdown();

        long testDuration = System.currentTimeMillis() - testStartTime;
        LockMetricsService.LockStatistics stats = metricsService.getLockStatistics();

        log.info("Lock metrics under load results:");
        log.info("  Test duration: {}ms", testDuration);
        log.info("  Total acquisitions: {}", stats.getTotalAcquisitions());
        log.info("  Total failures: {}", stats.getTotalFailures());
        log.info("  Total releases: {}", stats.getTotalReleases());
        log.info("  Success rate: {:.2f}%", stats.getSuccessRate());
        log.info("  Average acquisition time: {:.2f}ms", stats.getAverageAcquisitionTime());

        // Verify metrics integrity
        assertTrue(stats.getTotalAcquisitions() > 0, "Should have successful acquisitions");
        assertTrue(stats.getTotalAcquisitions() <= operationCount, "Acquisitions should not exceed operations");
        assertTrue(stats.getSuccessRate() >= 50.0, "Success rate should be at least 50% under load");
        assertTrue(stats.getAverageAcquisitionTime() < 100.0, "Average acquisition time should be reasonable");
    }
}