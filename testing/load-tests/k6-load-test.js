/**
 * Ocean Shopping Center - Load Testing Script
 * K6 Load Testing Configuration for 10,000+ Concurrent Users
 */

import http from 'k6/http';
import ws from 'k6/ws';
import { check, group, sleep, fail } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// Custom metrics
const loginSuccess = new Rate('login_success');
const productLoadTime = new Trend('product_load_time');
const cartOperations = new Counter('cart_operations');
const checkoutTime = new Trend('checkout_time');
const websocketConnections = new Counter('websocket_connections');
const apiErrors = new Rate('api_errors');

// Test configuration
export const options = {
  stages: [
    // Warm-up phase
    { duration: '2m', target: 100 },
    { duration: '3m', target: 500 },
    
    // Ramp-up to target load
    { duration: '5m', target: 2000 },
    { duration: '5m', target: 5000 },
    { duration: '5m', target: 8000 },
    { duration: '5m', target: 10000 },
    
    // Sustained load
    { duration: '10m', target: 10000 },
    { duration: '5m', target: 12000 }, // Peak load test
    
    // Ramp-down
    { duration: '5m', target: 5000 },
    { duration: '3m', target: 1000 },
    { duration: '2m', target: 0 },
  ],
  
  thresholds: {
    // Performance requirements
    'http_req_duration': ['p(95)<200', 'p(99)<500'],
    'http_req_failed': ['rate<0.1'], // Error rate < 10%
    'login_success': ['rate>0.95'], // Login success rate > 95%
    'product_load_time': ['p(95)<150'],
    'checkout_time': ['p(95)<1000'],
    'api_errors': ['rate<0.05'], // API error rate < 5%
    
    // System stability
    'http_reqs': ['rate>100'], // Minimum 100 requests per second
    'vus': ['value<15000'], // Maximum concurrent users limit
  },
  
  // Load test configuration
  discardResponseBodies: true,
  noConnectionReuse: false,
  userAgent: 'Ocean-Shopping-LoadTest/1.0',
};

// Environment configuration
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const FRONTEND_URL = __ENV.FRONTEND_URL || 'http://localhost:3001';
const WS_URL = __ENV.WS_URL || 'ws://localhost:8080';

// Test data
const users = [
  { email: 'user1@test.com', password: 'testpass123' },
  { email: 'user2@test.com', password: 'testpass123' },
  { email: 'user3@test.com', password: 'testpass123' },
  { email: 'admin@test.com', password: 'adminpass123' },
];

const products = [
  { id: 1, name: 'Laptop', price: 999.99 },
  { id: 2, name: 'Smartphone', price: 599.99 },
  { id: 3, name: 'Headphones', price: 199.99 },
  { id: 4, name: 'Tablet', price: 399.99 },
  { id: 5, name: 'Smart Watch', price: 299.99 },
];

// Utility functions
function randomUser() {
  return users[Math.floor(Math.random() * users.length)];
}

function randomProduct() {
  return products[Math.floor(Math.random() * products.length)];
}

function randomInt(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

// Authentication helper
function authenticate() {
  const user = randomUser();
  
  const loginPayload = JSON.stringify({
    email: user.email,
    password: user.password,
  });
  
  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };
  
  const response = http.post(`${BASE_URL}/api/auth/login`, loginPayload, params);
  
  const success = check(response, {
    'login status is 200': (r) => r.status === 200,
    'login response has token': (r) => JSON.parse(r.body).token !== undefined,
  });
  
  loginSuccess.add(success);
  
  if (success) {
    const token = JSON.parse(response.body).token;
    return {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    };
  }
  
  return null;
}

// Main test scenarios
export default function () {
  const scenario = Math.random();
  
  if (scenario < 0.4) {
    // 40% - Browse products scenario
    browseProductsScenario();
  } else if (scenario < 0.7) {
    // 30% - Shopping cart scenario
    shoppingCartScenario();
  } else if (scenario < 0.85) {
    // 15% - Checkout process scenario
    checkoutScenario();
  } else if (scenario < 0.95) {
    // 10% - Real-time features scenario
    realTimeFeaturesScenario();
  } else {
    // 5% - Admin operations scenario
    adminOperationsScenario();
  }
  
  sleep(randomInt(1, 3));
}

// Scenario implementations
function browseProductsScenario() {
  group('Browse Products Scenario', () => {
    const start = new Date().getTime();
    
    // Health check
    let response = http.get(`${BASE_URL}/actuator/health`);
    check(response, {
      'health check status is 200': (r) => r.status === 200,
    });
    
    // Browse product categories
    response = http.get(`${BASE_URL}/api/products/categories`);
    check(response, {
      'categories status is 200': (r) => r.status === 200,
      'categories response has data': (r) => JSON.parse(r.body).length > 0,
    });
    
    // Search products
    const searchTerms = ['laptop', 'phone', 'headphones', 'tablet'];
    const searchTerm = searchTerms[Math.floor(Math.random() * searchTerms.length)];
    
    response = http.get(`${BASE_URL}/api/products/search?q=${searchTerm}&page=0&size=20`);
    check(response, {
      'search status is 200': (r) => r.status === 200,
      'search results exist': (r) => JSON.parse(r.body).content !== undefined,
    });
    
    // Get product details
    const productId = randomInt(1, 50);
    response = http.get(`${BASE_URL}/api/products/${productId}`);
    check(response, {
      'product details status is 200 or 404': (r) => [200, 404].includes(r.status),
    });
    
    if (response.status === 200) {
      // Get product reviews
      response = http.get(`${BASE_URL}/api/products/${productId}/reviews`);
      check(response, {
        'product reviews status is 200': (r) => r.status === 200,
      });
    }
    
    const end = new Date().getTime();
    productLoadTime.add(end - start);
  });
}

function shoppingCartScenario() {
  group('Shopping Cart Scenario', () => {
    const headers = authenticate();
    if (!headers) {
      fail('Authentication failed for shopping cart scenario');
    }
    
    // Get current cart
    let response = http.get(`${BASE_URL}/api/cart`, { headers });
    check(response, {
      'get cart status is 200': (r) => r.status === 200,
    });
    
    // Add items to cart
    for (let i = 0; i < randomInt(1, 3); i++) {
      const product = randomProduct();
      const cartItem = JSON.stringify({
        productId: product.id,
        quantity: randomInt(1, 3),
      });
      
      response = http.post(`${BASE_URL}/api/cart/items`, cartItem, { headers });
      check(response, {
        'add to cart status is 200': (r) => r.status === 200,
      });
      
      cartOperations.add(1);
    }
    
    // Update cart item
    response = http.get(`${BASE_URL}/api/cart`, { headers });
    if (response.status === 200) {
      const cart = JSON.parse(response.body);
      if (cart.items && cart.items.length > 0) {
        const itemId = cart.items[0].id;
        const updatePayload = JSON.stringify({
          quantity: randomInt(2, 5),
        });
        
        response = http.put(`${BASE_URL}/api/cart/items/${itemId}`, updatePayload, { headers });
        check(response, {
          'update cart item status is 200': (r) => r.status === 200,
        });
        
        cartOperations.add(1);
      }
    }
    
    // Apply coupon (optional)
    if (Math.random() < 0.3) {
      const couponPayload = JSON.stringify({
        code: 'SAVE10',
      });
      
      response = http.post(`${BASE_URL}/api/cart/coupon`, couponPayload, { headers });
      check(response, {
        'apply coupon status is 200 or 400': (r) => [200, 400].includes(r.status),
      });
    }
  });
}

function checkoutScenario() {
  group('Checkout Scenario', () => {
    const start = new Date().getTime();
    const headers = authenticate();
    
    if (!headers) {
      fail('Authentication failed for checkout scenario');
    }
    
    // Get cart for checkout
    let response = http.get(`${BASE_URL}/api/cart`, { headers });
    if (response.status !== 200) {
      // Add a product to cart first
      const product = randomProduct();
      const cartItem = JSON.stringify({
        productId: product.id,
        quantity: 1,
      });
      
      http.post(`${BASE_URL}/api/cart/items`, cartItem, { headers });
    }
    
    // Create order
    const orderPayload = JSON.stringify({
      shippingAddress: {
        street: '123 Test Street',
        city: 'Test City',
        state: 'TS',
        zipCode: '12345',
        country: 'US',
      },
      billingAddress: {
        street: '123 Test Street',
        city: 'Test City',
        state: 'TS',
        zipCode: '12345',
        country: 'US',
      },
      paymentMethod: 'STRIPE',
    });
    
    response = http.post(`${BASE_URL}/api/orders`, orderPayload, { headers });
    check(response, {
      'create order status is 200': (r) => r.status === 200,
      'order has id': (r) => JSON.parse(r.body).id !== undefined,
    });
    
    if (response.status === 200) {
      const order = JSON.parse(response.body);
      
      // Simulate payment processing
      sleep(1); // Payment processing delay
      
      // Get order status
      response = http.get(`${BASE_URL}/api/orders/${order.id}`, { headers });
      check(response, {
        'get order status is 200': (r) => r.status === 200,
      });
    }
    
    const end = new Date().getTime();
    checkoutTime.add(end - start);
  });
}

function realTimeFeaturesScenario() {
  group('Real-time Features Scenario', () => {
    const headers = authenticate();
    if (!headers) return;
    
    // WebSocket connection test
    const wsUrl = `${WS_URL}/ws/chat`;
    const response = ws.connect(wsUrl, {}, function (socket) {
      websocketConnections.add(1);
      
      socket.on('open', () => {
        console.log('WebSocket connected');
        
        // Send a test message
        socket.send(JSON.stringify({
          type: 'message',
          content: 'Load test message',
        }));
      });
      
      socket.on('message', (data) => {
        console.log('Received message:', data);
      });
      
      socket.on('close', () => {
        console.log('WebSocket closed');
      });
      
      socket.on('error', (error) => {
        console.log('WebSocket error:', error);
      });
      
      // Keep connection alive for a short period
      sleep(5);
      socket.close();
    });
    
    // Get notifications
    const notificationResponse = http.get(`${BASE_URL}/api/notifications`, { headers });
    check(notificationResponse, {
      'notifications status is 200': (r) => r.status === 200,
    });
  });
}

function adminOperationsScenario() {
  group('Admin Operations Scenario', () => {
    // Try to authenticate as admin
    const adminPayload = JSON.stringify({
      email: 'admin@test.com',
      password: 'adminpass123',
    });
    
    let response = http.post(`${BASE_URL}/api/auth/login`, adminPayload, {
      headers: { 'Content-Type': 'application/json' },
    });
    
    if (response.status !== 200) return;
    
    const token = JSON.parse(response.body).token;
    const headers = {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    };
    
    // Get admin dashboard data
    response = http.get(`${BASE_URL}/api/admin/dashboard`, { headers });
    check(response, {
      'admin dashboard status is 200 or 403': (r) => [200, 403].includes(r.status),
    });
    
    // Get orders for admin
    response = http.get(`${BASE_URL}/api/admin/orders?page=0&size=20`, { headers });
    check(response, {
      'admin orders status is 200 or 403': (r) => [200, 403].includes(r.status),
    });
    
    // Get system metrics
    response = http.get(`${BASE_URL}/actuator/metrics`, { headers });
    check(response, {
      'metrics status is 200': (r) => r.status === 200,
    });
  });
}

// Error handling
export function handleSummary(data) {
  const apiErrorRate = data.metrics.api_errors ? data.metrics.api_errors.rate : 0;
  
  if (apiErrorRate > 0.05) {
    console.log(`⚠️ HIGH API ERROR RATE: ${(apiErrorRate * 100).toFixed(2)}%`);
  }
  
  return {
    'stdout': textSummary(data, { indent: ' ', enableColors: true }),
    'load-test-results.json': JSON.stringify(data),
    'load-test-report.html': htmlReport(data),
  };
}

// Helper function for text summary (simplified)
function textSummary(data, options) {
  const { indent = '', enableColors = false } = options;
  
  let summary = `${indent}Load Test Results Summary:\n`;
  summary += `${indent}============================\n`;
  
  // Request metrics
  if (data.metrics.http_reqs) {
    summary += `${indent}Total Requests: ${data.metrics.http_reqs.count}\n`;
    summary += `${indent}Request Rate: ${data.metrics.http_reqs.rate.toFixed(2)} req/s\n`;
  }
  
  // Response time metrics
  if (data.metrics.http_req_duration) {
    summary += `${indent}Response Time P95: ${data.metrics.http_req_duration.p95.toFixed(2)}ms\n`;
    summary += `${indent}Response Time P99: ${data.metrics.http_req_duration.p99.toFixed(2)}ms\n`;
  }
  
  // Error metrics
  if (data.metrics.http_req_failed) {
    const errorRate = data.metrics.http_req_failed.rate * 100;
    summary += `${indent}Error Rate: ${errorRate.toFixed(2)}%\n`;
  }
  
  // Custom metrics
  if (data.metrics.login_success) {
    const successRate = data.metrics.login_success.rate * 100;
    summary += `${indent}Login Success Rate: ${successRate.toFixed(2)}%\n`;
  }
  
  return summary;
}

// Simplified HTML report generator
function htmlReport(data) {
  return `
    <!DOCTYPE html>
    <html>
    <head>
      <title>Ocean Shopping Center - Load Test Report</title>
      <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .header { color: #2c3e50; border-bottom: 2px solid #3498db; padding-bottom: 10px; }
        .metric { margin: 10px 0; padding: 10px; background-color: #f8f9fa; border-radius: 5px; }
        .pass { color: #27ae60; }
        .fail { color: #e74c3c; }
      </style>
    </head>
    <body>
      <h1 class="header">Ocean Shopping Center - Load Test Report</h1>
      <div class="metric">
        <h3>Test Configuration</h3>
        <p>Target Load: 10,000+ concurrent users</p>
        <p>Duration: ~45 minutes</p>
        <p>Scenarios: Browse Products, Shopping Cart, Checkout, Real-time Features, Admin Operations</p>
      </div>
      
      <div class="metric">
        <h3>Performance Results</h3>
        <p>Total Requests: ${data.metrics.http_reqs?.count || 'N/A'}</p>
        <p>Request Rate: ${data.metrics.http_reqs?.rate?.toFixed(2) || 'N/A'} req/s</p>
        <p>P95 Response Time: ${data.metrics.http_req_duration?.p95?.toFixed(2) || 'N/A'}ms</p>
        <p>Error Rate: ${((data.metrics.http_req_failed?.rate || 0) * 100).toFixed(2)}%</p>
      </div>
      
      <div class="metric">
        <h3>Business Metrics</h3>
        <p>Login Success Rate: ${((data.metrics.login_success?.rate || 0) * 100).toFixed(2)}%</p>
        <p>Cart Operations: ${data.metrics.cart_operations?.count || 'N/A'}</p>
        <p>WebSocket Connections: ${data.metrics.websocket_connections?.count || 'N/A'}</p>
      </div>
      
      <div class="metric">
        <h3>Test Status</h3>
        <p class="${(data.metrics.http_req_duration?.p95 || 0) < 200 ? 'pass' : 'fail'}">
          P95 Response Time < 200ms: ${(data.metrics.http_req_duration?.p95 || 0) < 200 ? 'PASS' : 'FAIL'}
        </p>
        <p class="${(data.metrics.http_req_failed?.rate || 0) < 0.1 ? 'pass' : 'fail'}">
          Error Rate < 10%: ${(data.metrics.http_req_failed?.rate || 0) < 0.1 ? 'PASS' : 'FAIL'}
        </p>
      </div>
    </body>
    </html>
  `;
}