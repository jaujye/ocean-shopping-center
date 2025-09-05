package com.ocean.shopping.controller;

import com.ocean.shopping.dto.order.OrderResponse;
import com.ocean.shopping.dto.order.OrderSummaryResponse;
import com.ocean.shopping.dto.order.OrderStatusUpdateRequest;
import com.ocean.shopping.model.entity.enums.OrderStatus;
import com.ocean.shopping.security.JwtAuthenticationToken;
import com.ocean.shopping.service.OrderManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for admin order management
 */
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final OrderManagementService orderManagementService;

    /**
     * Get all orders with filtering and pagination
     */
    @GetMapping
    public ResponseEntity<Page<OrderSummaryResponse>> getAllOrders(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime endDate,
            @RequestParam(required = false) Long storeId,
            Authentication authentication) {
        
        try {
            UUID adminId = ((JwtAuthenticationToken) authentication).getUserId();
            log.debug("Admin {} getting all orders with filters - status: {}, dates: {} to {}, store: {}", 
                adminId, status, startDate, endDate, storeId);

            Page<OrderSummaryResponse> orders = orderManagementService.getAllOrders(
                pageable, status, startDate, endDate);

            return ResponseEntity.ok(orders);

        } catch (Exception e) {
            log.error("Error getting all orders: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Search all orders
     */
    @GetMapping("/search")
    public ResponseEntity<Page<OrderSummaryResponse>> searchAllOrders(
            @RequestParam String query,
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication) {
        
        try {
            UUID adminId = ((JwtAuthenticationToken) authentication).getUserId();
            log.debug("Admin {} searching orders with query: {}", adminId, query);

            Page<OrderSummaryResponse> orders = orderManagementService.searchAllOrders(query, pageable);
            return ResponseEntity.ok(orders);

        } catch (Exception e) {
            log.error("Error searching orders: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get specific order details for admin
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderForAdmin(
            @PathVariable Long orderId,
            Authentication authentication) {
        
        try {
            UUID adminId = ((JwtAuthenticationToken) authentication).getUserId();
            log.debug("Admin {} getting order: {}", adminId, orderId);

            OrderResponse order = orderManagementService.getOrderForAdmin(orderId);
            return ResponseEntity.ok(order);

        } catch (Exception e) {
            log.error("Error getting order for admin: {}", e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get all orders requiring attention
     */
    @GetMapping("/attention")
    public ResponseEntity<List<OrderSummaryResponse>> getOrdersRequiringAttention(
            Authentication authentication) {
        
        try {
            UUID adminId = ((JwtAuthenticationToken) authentication).getUserId();
            log.debug("Admin {} getting orders requiring attention", adminId);

            List<OrderSummaryResponse> orders = orderManagementService.getOrdersRequiringAttention();
            return ResponseEntity.ok(orders);

        } catch (Exception e) {
            log.error("Error getting orders requiring attention: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update order status (admin override)
     */
    @PutMapping("/{orderId}/status")
    public ResponseEntity<String> updateOrderStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderStatusUpdateRequest request,
            Authentication authentication) {
        
        try {
            UUID adminId = ((JwtAuthenticationToken) authentication).getUserId();
            log.info("Admin {} updating order {} status to {}", adminId, orderId, request.getStatus());

            orderManagementService.updateOrderStatus(orderId, request, adminId);
            return ResponseEntity.ok("Order status updated successfully");

        } catch (Exception e) {
            log.error("Error updating order status: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Failed to update order status: " + e.getMessage());
        }
    }

    /**
     * Process refund for an order (admin)
     */
    @PostMapping("/{orderId}/refund")
    public ResponseEntity<String> processRefund(
            @PathVariable Long orderId,
            @RequestParam BigDecimal amount,
            @RequestParam String reason,
            Authentication authentication) {
        
        try {
            UUID adminId = ((JwtAuthenticationToken) authentication).getUserId();
            log.info("Admin {} processing refund of {} for order {}", adminId, amount, orderId);

            orderManagementService.processRefund(orderId, amount, reason, adminId);
            return ResponseEntity.ok("Refund processed successfully");

        } catch (Exception e) {
            log.error("Error processing refund: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Failed to process refund: " + e.getMessage());
        }
    }

    /**
     * Get comprehensive revenue analytics
     */
    @GetMapping("/analytics/revenue")
    public ResponseEntity<Map<String, Object>> getRevenueAnalytics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime endDate,
            Authentication authentication) {
        
        try {
            UUID adminId = ((JwtAuthenticationToken) authentication).getUserId();
            log.debug("Admin {} getting revenue analytics from {} to {}", adminId, startDate, endDate);

            Map<String, Object> analytics = orderManagementService.getRevenueAnalytics(startDate, endDate);
            return ResponseEntity.ok(analytics);

        } catch (Exception e) {
            log.error("Error getting revenue analytics: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get order statistics dashboard
     */
    @GetMapping("/analytics/dashboard")
    public ResponseEntity<?> getOrderDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime endDate,
            Authentication authentication) {
        
        try {
            UUID adminId = ((JwtAuthenticationToken) authentication).getUserId();
            log.debug("Admin {} getting order dashboard", adminId);

            // This would require implementation of dashboard statistics
            // For now, return placeholder
            return ResponseEntity.ok("Order dashboard endpoint - requires implementation");

        } catch (Exception e) {
            log.error("Error getting order dashboard: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get problematic orders (cancelled, returned, disputed)
     */
    @GetMapping("/problematic")
    public ResponseEntity<Page<OrderSummaryResponse>> getProblematicOrders(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime endDate,
            Authentication authentication) {
        
        try {
            UUID adminId = ((JwtAuthenticationToken) authentication).getUserId();
            log.debug("Admin {} getting problematic orders", adminId);

            // Get cancelled and returned orders
            List<OrderStatus> problematicStatuses = List.of(OrderStatus.CANCELLED, OrderStatus.RETURNED);
            
            // This would require filtering by multiple statuses in the service
            Page<OrderSummaryResponse> orders = orderManagementService.getAllOrders(
                pageable, OrderStatus.CANCELLED, startDate, endDate);

            return ResponseEntity.ok(orders);

        } catch (Exception e) {
            log.error("Error getting problematic orders: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Export orders to CSV (admin)
     */
    @GetMapping("/export")
    public ResponseEntity<?> exportAllOrders(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime endDate,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) Long storeId,
            Authentication authentication) {
        
        try {
            UUID adminId = ((JwtAuthenticationToken) authentication).getUserId();
            log.info("Admin {} exporting orders", adminId);

            // This would require implementation of export functionality
            return ResponseEntity.ok("Order export endpoint - requires implementation");

        } catch (Exception e) {
            log.error("Error exporting orders: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get system-wide order metrics
     */
    @GetMapping("/metrics")
    public ResponseEntity<?> getOrderMetrics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime endDate,
            Authentication authentication) {
        
        try {
            UUID adminId = ((JwtAuthenticationToken) authentication).getUserId();
            log.debug("Admin {} getting order metrics", adminId);

            // This would provide system-wide metrics
            return ResponseEntity.ok("Order metrics endpoint - requires implementation");

        } catch (Exception e) {
            log.error("Error getting order metrics: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Force order status change (emergency admin function)
     */
    @PutMapping("/{orderId}/force-status")
    public ResponseEntity<String> forceOrderStatus(
            @PathVariable Long orderId,
            @RequestParam OrderStatus status,
            @RequestParam String reason,
            Authentication authentication) {
        
        try {
            UUID adminId = ((JwtAuthenticationToken) authentication).getUserId();
            log.warn("Admin {} force changing order {} status to {} - reason: {}", 
                adminId, orderId, status, reason);

            // Create status update request
            OrderStatusUpdateRequest request = new OrderStatusUpdateRequest();
            request.setStatus(status);
            request.setInternalNotes("FORCE STATUS CHANGE - " + reason);
            request.setNotifyCustomer(true);

            orderManagementService.updateOrderStatus(orderId, request, adminId);
            return ResponseEntity.ok("Order status force updated successfully");

        } catch (Exception e) {
            log.error("Error force updating order status: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Failed to force update order status: " + e.getMessage());
        }
    }

    /**
     * Get fraud detection alerts
     */
    @GetMapping("/fraud-alerts")
    public ResponseEntity<?> getFraudAlerts(Authentication authentication) {
        
        try {
            UUID adminId = ((JwtAuthenticationToken) authentication).getUserId();
            log.debug("Admin {} getting fraud alerts", adminId);

            // This would integrate with fraud detection system
            return ResponseEntity.ok("Fraud alerts endpoint - requires implementation");

        } catch (Exception e) {
            log.error("Error getting fraud alerts: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Bulk operations on orders
     */
    @PostMapping("/bulk-action")
    public ResponseEntity<String> bulkAction(
            @RequestParam List<Long> orderIds,
            @RequestParam String action,
            @RequestParam(required = false) String parameters,
            Authentication authentication) {
        
        try {
            UUID adminId = ((JwtAuthenticationToken) authentication).getUserId();
            log.info("Admin {} performing bulk action '{}' on {} orders", adminId, action, orderIds.size());

            // This would require implementation of various bulk operations
            return ResponseEntity.ok("Bulk action endpoint - requires implementation");

        } catch (Exception e) {
            log.error("Error performing bulk action: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Failed to perform bulk action: " + e.getMessage());
        }
    }
}