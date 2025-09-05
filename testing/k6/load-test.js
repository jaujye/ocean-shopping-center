import http from 'k6/http';
import ws from 'k6/ws';
import { check, group, sleep } from 'k6';
import { Rate, Counter, Trend } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const wsConnections = new Counter('websocket_connections');
const responseTimeTrend = new Trend('response_time');

// Configuration
const BASE_URL = __ENV.BASE_URL || 'http://backend:8080';
const WS_URL = __ENV.WS_URL || 'ws://backend:8080/ws';

// Test scenarios
export const options = {
  scenarios: {
    // Light load - constant users browsing
    browsing: {
      executor: 'constant-vus',
      vus: 100,
      duration: '30m',
      exec: 'browsingFlow',
    },
    
    // Spike test - sudden traffic increase
    spike: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '2m', target: 100 },
        { duration: '5m', target: 100 },
        { duration: '2m', target: 1000 },
        { duration: '5m', target: 1000 },
        { duration: '2m', target: 100 },
        { duration: '5m', target: 100 },
        { duration: '2m', target: 0 },
      ],
      exec: 'shoppingFlow',
    },
    
    // Stress test - find breaking point
    stress: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '10m', target: 1000 },
        { duration: '20m', target: 2000 },
        { duration: '10m', target: 3000 },
        { duration: '10m', target: 5000 },
        { duration: '10m', target: 0 },
      ],
      exec: 'stressFlow',
    },
    
    // WebSocket connections test
    websocket_test: {
      executor: 'constant-vus',
      vus: 500,
      duration: '10m',
      exec: 'websocketFlow',
    }
  },
  
  thresholds: {
    http_req_duration: ['p(95)<200'], // 95% of requests must complete below 200ms
    http_req_failed: ['rate<0.01'],   // Error rate must be below 1%
    errors: ['rate<0.01'],            // Custom error rate threshold
    response_time: ['p(99)<500'],     // 99% response time under 500ms
  },
};

// User authentication helper
function authenticate() {
  const loginPayload = {
    username: `user${Math.floor(Math.random() * 10000)}@oceanshop.com`,
    password: 'TestPassword123!',
  };

  const response = http.post(`${BASE_URL}/api/auth/login`, JSON.stringify(loginPayload), {
    headers: { 'Content-Type': 'application/json' },
  });

  if (response.status === 200) {
    const token = JSON.parse(response.body).token;
    return { Authorization: `Bearer ${token}` };
  }
  return null;
}

// Test user registration
function registerUser() {
  const userData = {
    username: `testuser${Math.floor(Math.random() * 100000)}`,
    email: `test${Math.floor(Math.random() * 100000)}@oceanshop.com`,
    password: 'TestPassword123!',
    firstName: 'Test',
    lastName: 'User',
  };

  const response = http.post(`${BASE_URL}/api/auth/register`, JSON.stringify(userData), {
    headers: { 'Content-Type': 'application/json' },
  });

  return check(response, {
    'registration successful': (r) => r.status === 201,
    'response time < 500ms': (r) => r.timings.duration < 500,
  });
}

// Browsing flow - lightweight user behavior
export function browsingFlow() {
  group('User Browsing Flow', function() {
    // Home page
    let response = http.get(`${BASE_URL}/`);
    check(response, {
      'home page loads': (r) => r.status === 200,
      'home page response time < 1s': (r) => r.timings.duration < 1000,
    });
    responseTimeTrend.add(response.timings.duration);
    
    sleep(1);
    
    // Browse products
    response = http.get(`${BASE_URL}/api/products?page=0&size=20`);
    check(response, {
      'products load': (r) => r.status === 200,
      'products response time < 200ms': (r) => r.timings.duration < 200,
    });
    responseTimeTrend.add(response.timings.duration);
    
    sleep(2);
    
    // View product details
    const productId = Math.floor(Math.random() * 100) + 1;
    response = http.get(`${BASE_URL}/api/products/${productId}`);
    check(response, {
      'product details load': (r) => r.status === 200 || r.status === 404,
      'product details response time < 200ms': (r) => r.timings.duration < 200,
    });
    responseTimeTrend.add(response.timings.duration);
    
    sleep(1);
    
    // Search products
    const searchTerms = ['shirt', 'shoes', 'electronics', 'books', 'toys'];
    const searchTerm = searchTerms[Math.floor(Math.random() * searchTerms.length)];
    response = http.get(`${BASE_URL}/api/products/search?q=${searchTerm}`);
    check(response, {
      'search works': (r) => r.status === 200,
      'search response time < 300ms': (r) => r.timings.duration < 300,
    });
    responseTimeTrend.add(response.timings.duration);
    
    if (!check(response, {'no errors': (r) => r.status < 400})) {
      errorRate.add(1);
    } else {
      errorRate.add(0);
    }
  });
}

// Shopping flow - complete purchase process
export function shoppingFlow() {
  group('Complete Shopping Flow', function() {
    // Register user
    if (Math.random() < 0.1) { // 10% new users
      registerUser();
    }
    
    // Authenticate
    const authHeaders = authenticate();
    if (!authHeaders) {
      errorRate.add(1);
      return;
    }
    
    // Add items to cart
    const productIds = [1, 2, 3, 4, 5];
    productIds.forEach((productId, index) => {
      const cartPayload = {
        productId: productId,
        quantity: Math.floor(Math.random() * 3) + 1,
      };
      
      const response = http.post(`${BASE_URL}/api/cart/add`, JSON.stringify(cartPayload), {
        headers: { ...authHeaders, 'Content-Type': 'application/json' },
      });
      
      check(response, {
        'add to cart successful': (r) => r.status === 200,
        'add to cart response time < 200ms': (r) => r.timings.duration < 200,
      });
      responseTimeTrend.add(response.timings.duration);
      
      if (index < productIds.length - 1) sleep(0.5);
    });
    
    // View cart
    let response = http.get(`${BASE_URL}/api/cart`, {
      headers: authHeaders,
    });
    check(response, {
      'cart loads': (r) => r.status === 200,
      'cart response time < 200ms': (r) => r.timings.duration < 200,
    });
    responseTimeTrend.add(response.timings.duration);
    
    sleep(1);
    
    // Apply coupon (30% chance)
    if (Math.random() < 0.3) {
      const couponPayload = { code: 'SAVE10' };
      response = http.post(`${BASE_URL}/api/cart/coupon`, JSON.stringify(couponPayload), {
        headers: { ...authHeaders, 'Content-Type': 'application/json' },
      });
      check(response, {
        'coupon application handled': (r) => r.status === 200 || r.status === 400,
      });
      responseTimeTrend.add(response.timings.duration);
    }
    
    // Checkout process
    const checkoutPayload = {
      shippingAddress: {
        street: '123 Test St',
        city: 'Test City',
        state: 'TS',
        zipCode: '12345',
        country: 'US'
      },
      paymentMethod: 'credit_card',
      paymentDetails: {
        cardNumber: '4111111111111111',
        expiryMonth: '12',
        expiryYear: '2025',
        cvv: '123'
      }
    };
    
    response = http.post(`${BASE_URL}/api/checkout`, JSON.stringify(checkoutPayload), {
      headers: { ...authHeaders, 'Content-Type': 'application/json' },
    });
    
    check(response, {
      'checkout processed': (r) => r.status === 200 || r.status === 201,
      'checkout response time < 2s': (r) => r.timings.duration < 2000,
    });
    responseTimeTrend.add(response.timings.duration);
    
    if (!check(response, {'no errors': (r) => r.status < 400})) {
      errorRate.add(1);
    } else {
      errorRate.add(0);
    }
  });
}

// Stress test flow - high intensity operations
export function stressFlow() {
  group('Stress Test Flow', function() {
    // Rapid API calls
    for (let i = 0; i < 10; i++) {
      const response = http.get(`${BASE_URL}/api/products?page=${i}&size=50`);
      check(response, {
        'stress test response': (r) => r.status === 200,
        'stress response time < 1s': (r) => r.timings.duration < 1000,
      });
      responseTimeTrend.add(response.timings.duration);
      
      if (!check(response, {'no errors': (r) => r.status < 400})) {
        errorRate.add(1);
      } else {
        errorRate.add(0);
      }
    }
    
    sleep(0.1); // Minimal sleep for stress test
  });
}

// WebSocket connection test
export function websocketFlow() {
  group('WebSocket Flow', function() {
    const response = ws.connect(WS_URL, {}, function(socket) {
      wsConnections.add(1);
      
      socket.on('open', function() {
        // Send periodic messages
        const interval = setInterval(() => {
          if (socket.readyState === WebSocket.OPEN) {
            socket.send(JSON.stringify({
              type: 'HEARTBEAT',
              timestamp: Date.now()
            }));
          }
        }, 30000);
        
        socket.setTimeout(() => {
          clearInterval(interval);
          socket.close();
        }, 60000); // Keep connection for 1 minute
      });
      
      socket.on('message', function(message) {
        check(message, {
          'websocket message received': (msg) => msg.length > 0,
        });
      });
      
      socket.on('error', function(error) {
        errorRate.add(1);
      });
    });
    
    check(response, {
      'websocket connected': (r) => r && r.status === 101,
    });
  });
}

// Custom teardown function
export function teardown(data) {
  console.log('Load test completed');
  console.log(`Error rate: ${errorRate.rate * 100}%`);
  console.log(`WebSocket connections: ${wsConnections.count}`);
}