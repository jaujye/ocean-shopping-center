import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Rate, Counter, Trend, Gauge } from 'k6/metrics';

// Custom metrics for performance validation
const errorRate = new Rate('errors');
const successfulRequests = new Counter('successful_requests');
const responseTimeTrend = new Trend('response_time_custom');
const activeUsers = new Gauge('active_users');

// Performance targets from requirements
const PERFORMANCE_TARGETS = {
    API_RESPONSE_P99: 200,      // milliseconds
    DATABASE_QUERY_P95: 100,    // milliseconds  
    CACHE_HIT_RATIO: 0.95,      // 95%
    PAGE_LOAD_TIME: 2000,       // milliseconds
    CONCURRENT_USERS: 10000,    // users
    ERROR_RATE_MAX: 0.01        // 1%
};

// Configuration
const BASE_URL = __ENV.BASE_URL || 'http://backend:8080';
const FRONTEND_URL = __ENV.FRONTEND_URL || 'http://frontend:80';
const TARGET_USERS = parseInt(__ENV.TARGET_USERS || '1000');
const TEST_DURATION = __ENV.TEST_DURATION || '10m';

export const options = {
    scenarios: {
        // Performance validation scenario
        performance_validation: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '2m', target: Math.floor(TARGET_USERS * 0.1) }, // Ramp up to 10%
                { duration: '3m', target: Math.floor(TARGET_USERS * 0.5) }, // Ramp up to 50%
                { duration: '5m', target: TARGET_USERS },                   // Full load
                { duration: TEST_DURATION, target: TARGET_USERS },          // Sustain full load
                { duration: '2m', target: 0 },                             // Ramp down
            ],
            exec: 'performanceTest',
        },
        
        // Database performance test
        database_performance: {
            executor: 'constant-vus',
            vus: 50,
            duration: TEST_DURATION,
            exec: 'databasePerformanceTest',
        },
        
        // Cache performance test
        cache_performance: {
            executor: 'constant-vus', 
            vus: 100,
            duration: TEST_DURATION,
            exec: 'cachePerformanceTest',
        },
        
        // Frontend performance test
        frontend_performance: {
            executor: 'constant-vus',
            vus: 200,
            duration: TEST_DURATION,
            exec: 'frontendPerformanceTest',
        },
    },
    
    thresholds: {
        // API response time requirements
        'http_req_duration{scenario:performance_validation}': [
            `p(99)<${PERFORMANCE_TARGETS.API_RESPONSE_P99}`,
        ],
        
        // Error rate requirements
        errors: [`rate<${PERFORMANCE_TARGETS.ERROR_RATE_MAX}`],
        
        // Success rate requirements
        http_req_failed: ['rate<0.01'],
        
        // Custom response time tracking
        response_time_custom: [
            'p(95)<150',
            `p(99)<${PERFORMANCE_TARGETS.API_RESPONSE_P99}`,
        ],
        
        // Database query performance
        'http_req_duration{endpoint:database}': [
            `p(95)<${PERFORMANCE_TARGETS.DATABASE_QUERY_P95}`,
        ],
        
        // Cache hit ratio (simulated via response times)
        'http_req_duration{endpoint:cache}': [
            'p(95)<50', // Fast cache responses
        ],
        
        // Frontend load time
        'http_req_duration{scenario:frontend_performance}': [
            `p(95)<${PERFORMANCE_TARGETS.PAGE_LOAD_TIME}`,
        ],
    },
};

// Authentication helper
function authenticate() {
    const response = http.post(
        `${BASE_URL}/api/auth/login`,
        JSON.stringify({
            username: `perftest${Math.floor(Math.random() * 10000)}@ocean.com`,
            password: 'PerfTest123!',
        }),
        {
            headers: { 'Content-Type': 'application/json' },
            tags: { endpoint: 'auth' },
        }
    );
    
    if (response.status === 200) {
        const token = JSON.parse(response.body).token;
        return { Authorization: `Bearer ${token}` };
    }
    return null;
}

// Main performance validation test
export function performanceTest() {
    group('Performance Validation Test', function() {
        activeUsers.add(1);
        
        // Test API endpoints with performance requirements
        group('API Performance Tests', function() {
            // Product catalog endpoint (high-frequency)
            let response = http.get(`${BASE_URL}/api/products?page=0&size=20&sort=popular`, {
                tags: { endpoint: 'products' },
            });
            
            let passed = check(response, {
                'products API responds in time': (r) => r.timings.duration < PERFORMANCE_TARGETS.API_RESPONSE_P99,
                'products API returns 200': (r) => r.status === 200,
                'products API returns data': (r) => JSON.parse(r.body).content.length > 0,
            });
            
            responseTimeTrend.add(response.timings.duration);
            errorRate.add(!passed);
            if (passed) successfulRequests.add(1);
            
            sleep(0.5);
            
            // Product search endpoint
            const searchTerms = ['shirt', 'shoes', 'electronics', 'books'];
            const searchTerm = searchTerms[Math.floor(Math.random() * searchTerms.length)];
            
            response = http.get(`${BASE_URL}/api/products/search?q=${searchTerm}&limit=10`, {
                tags: { endpoint: 'search' },
            });
            
            passed = check(response, {
                'search API responds in time': (r) => r.timings.duration < PERFORMANCE_TARGETS.API_RESPONSE_P99,
                'search API returns 200': (r) => r.status === 200,
            });
            
            responseTimeTrend.add(response.timings.duration);
            errorRate.add(!passed);
            if (passed) successfulRequests.add(1);
            
            sleep(1);
            
            // Product details endpoint  
            const productId = Math.floor(Math.random() * 100) + 1;
            response = http.get(`${BASE_URL}/api/products/${productId}`, {
                tags: { endpoint: 'product_detail' },
            });
            
            passed = check(response, {
                'product detail responds in time': (r) => r.timings.duration < PERFORMANCE_TARGETS.API_RESPONSE_P99,
                'product detail status ok': (r) => r.status === 200 || r.status === 404,
            });
            
            responseTimeTrend.add(response.timings.duration);
            errorRate.add(!passed);
            if (passed) successfulRequests.add(1);
        });
        
        // Test authenticated operations
        group('Authenticated Operations', function() {
            const authHeaders = authenticate();
            
            if (authHeaders) {
                // Shopping cart operations
                response = http.get(`${BASE_URL}/api/cart`, {
                    headers: authHeaders,
                    tags: { endpoint: 'cart' },
                });
                
                passed = check(response, {
                    'cart API responds in time': (r) => r.timings.duration < PERFORMANCE_TARGETS.API_RESPONSE_P99,
                    'cart API returns 200': (r) => r.status === 200,
                });
                
                responseTimeTrend.add(response.timings.duration);
                errorRate.add(!passed);
                if (passed) successfulRequests.add(1);
                
                // Add item to cart
                response = http.post(
                    `${BASE_URL}/api/cart/add`,
                    JSON.stringify({
                        productId: Math.floor(Math.random() * 50) + 1,
                        quantity: Math.floor(Math.random() * 3) + 1,
                    }),
                    {
                        headers: { ...authHeaders, 'Content-Type': 'application/json' },
                        tags: { endpoint: 'cart_add' },
                    }
                );
                
                passed = check(response, {
                    'add to cart responds in time': (r) => r.timings.duration < PERFORMANCE_TARGETS.API_RESPONSE_P99,
                    'add to cart successful': (r) => r.status === 200,
                });
                
                responseTimeTrend.add(response.timings.duration);
                errorRate.add(!passed);
                if (passed) successfulRequests.add(1);
            }
        });
        
        activeUsers.add(-1);
    });
}

// Database performance-focused test
export function databasePerformanceTest() {
    group('Database Performance Test', function() {
        // Test database-heavy operations
        const response = http.get(`${BASE_URL}/api/products/categories/stats`, {
            tags: { endpoint: 'database' },
        });
        
        const passed = check(response, {
            'database query under target': (r) => r.timings.duration < PERFORMANCE_TARGETS.DATABASE_QUERY_P95,
            'database query returns 200': (r) => r.status === 200,
            'database query returns data': (r) => r.body.length > 0,
        });
        
        responseTimeTrend.add(response.timings.duration);
        errorRate.add(!passed);
        if (passed) successfulRequests.add(1);
    });
    
    sleep(1);
}

// Cache performance test
export function cachePerformanceTest() {
    group('Cache Performance Test', function() {
        // Test frequently cached endpoints
        const response = http.get(`${BASE_URL}/api/products/featured`, {
            tags: { endpoint: 'cache' },
        });
        
        const passed = check(response, {
            'cached response is fast': (r) => r.timings.duration < 50, // Should be fast if cached
            'cached response returns 200': (r) => r.status === 200,
            'cached response has cache headers': (r) => r.headers['Cache-Control'] !== undefined,
        });
        
        responseTimeTrend.add(response.timings.duration);
        errorRate.add(!passed);
        if (passed) successfulRequests.add(1);
    });
    
    sleep(0.5);
}

// Frontend performance test
export function frontendPerformanceTest() {
    group('Frontend Performance Test', function() {
        // Test frontend page load times
        const response = http.get(`${FRONTEND_URL}/`, {
            tags: { endpoint: 'frontend' },
        });
        
        const passed = check(response, {
            'frontend loads in time': (r) => r.timings.duration < PERFORMANCE_TARGETS.PAGE_LOAD_TIME,
            'frontend returns 200': (r) => r.status === 200,
            'frontend returns HTML': (r) => r.headers['Content-Type'].includes('text/html'),
        });
        
        responseTimeTrend.add(response.timings.duration);
        errorRate.add(!passed);
        if (passed) successfulRequests.add(1);
    });
    
    sleep(2);
}

// Setup function - runs once before test
export function setup() {
    console.log('Starting Ocean Shopping Center Performance Test');
    console.log(`Target: ${TARGET_USERS} users for ${TEST_DURATION}`);
    console.log(`Performance Targets:`);
    console.log(`- API Response P99: ${PERFORMANCE_TARGETS.API_RESPONSE_P99}ms`);
    console.log(`- Database Query P95: ${PERFORMANCE_TARGETS.DATABASE_QUERY_P95}ms`);
    console.log(`- Cache Hit Ratio: ${PERFORMANCE_TARGETS.CACHE_HIT_RATIO * 100}%`);
    console.log(`- Page Load Time: ${PERFORMANCE_TARGETS.PAGE_LOAD_TIME}ms`);
    
    // Validate endpoints are accessible
    const healthCheck = http.get(`${BASE_URL}/actuator/health`);
    if (healthCheck.status !== 200) {
        console.error('Backend health check failed');
        return null;
    }
    
    return {
        startTime: new Date(),
        baseUrl: BASE_URL,
        frontendUrl: FRONTEND_URL,
    };
}

// Teardown function - runs once after test
export function teardown(data) {
    if (data) {
        const endTime = new Date();
        const duration = (endTime - data.startTime) / 1000;
        
        console.log(`\n=== Performance Test Summary ===`);
        console.log(`Duration: ${duration} seconds`);
        console.log(`Target Users: ${TARGET_USERS}`);
        
        // Performance validation summary
        console.log(`\n=== Performance Target Validation ===`);
        console.log(`API Response P99 Target: ${PERFORMANCE_TARGETS.API_RESPONSE_P99}ms`);
        console.log(`Database Query P95 Target: ${PERFORMANCE_TARGETS.DATABASE_QUERY_P95}ms`);
        console.log(`Error Rate Target: ${PERFORMANCE_TARGETS.ERROR_RATE_MAX * 100}%`);
        console.log(`Cache Hit Ratio Target: ${PERFORMANCE_TARGETS.CACHE_HIT_RATIO * 100}%`);
        console.log(`Page Load Time Target: ${PERFORMANCE_TARGETS.PAGE_LOAD_TIME}ms`);
        
        console.log(`\nReview detailed metrics in Grafana dashboard`);
    }
}