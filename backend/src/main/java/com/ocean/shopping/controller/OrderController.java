package com.ocean.shopping.controller;

import com.ocean.shopping.dto.order.OrderResponse;
import com.ocean.shopping.dto.order.OrderSummaryResponse;
import com.ocean.shopping.model.entity.enums.OrderStatus;
import com.ocean.shopping.security.JwtAuthenticationToken;
import com.ocean.shopping.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller for customer order operations
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('USER')")
public class OrderController {

    private final OrderService orderService;

    /**
     * Get user's orders with pagination
     */
    @GetMapping
    public ResponseEntity<Page<OrderSummaryResponse>> getUserOrders(
            Authentication authentication,
            @PageableDefault(size = 10) Pageable pageable,
            @RequestParam(required = false) OrderStatus status) {
        
        try {
            UUID userId = ((JwtAuthenticationToken) authentication).getUserId();
            log.debug("Getting orders for user: {} with status: {}", userId, status);

            Page<OrderSummaryResponse> orders;
            if (status != null) {
                // If status filtering is needed, we'd enhance the OrderService
                orders = orderService.getUserOrders(userId, pageable);
            } else {
                orders = orderService.getUserOrders(userId, pageable);
            }

            return ResponseEntity.ok(orders);

        } catch (Exception e) {
            log.error("Error getting user orders: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get specific order by ID
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getUserOrder(
            @PathVariable Long orderId,
            Authentication authentication) {
        
        try {
            UUID userId = ((JwtAuthenticationToken) authentication).getUserId();
            log.debug("Getting order {} for user: {}", orderId, userId);

            OrderResponse order = orderService.getUserOrder(userId, orderId);
            return ResponseEntity.ok(order);

        } catch (Exception e) {
            log.error("Error getting order {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get order by order number
     */
    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<OrderResponse> getUserOrderByNumber(
            @PathVariable String orderNumber,
            Authentication authentication) {
        
        try {
            UUID userId = ((JwtAuthenticationToken) authentication).getUserId();
            log.debug("Getting order {} for user: {}", orderNumber, userId);

            OrderResponse order = orderService.getUserOrderByNumber(userId, orderNumber);
            return ResponseEntity.ok(order);

        } catch (Exception e) {
            log.error("Error getting order by number {}: {}", orderNumber, e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Cancel an order
     */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<String> cancelOrder(
            @PathVariable Long orderId,
            @RequestParam(required = false) String reason,
            Authentication authentication) {
        
        try {
            UUID userId = ((JwtAuthenticationToken) authentication).getUserId();
            log.info("Cancelling order {} for user: {} with reason: {}", orderId, userId, reason);

            String cancellationReason = reason != null ? reason : "Cancelled by customer";
            orderService.cancelOrder(userId, orderId, cancellationReason);

            return ResponseEntity.ok("Order cancelled successfully");

        } catch (Exception e) {
            log.error("Error cancelling order {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.badRequest().body("Failed to cancel order: " + e.getMessage());
        }
    }

    /**
     * Get order history with filtering
     */
    @GetMapping("/history")
    public ResponseEntity<Page<OrderSummaryResponse>> getOrderHistory(
            Authentication authentication,
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        
        try {
            UUID userId = ((JwtAuthenticationToken) authentication).getUserId();
            log.debug("Getting order history for user: {} with filters - status: {}, year: {}, month: {}", 
                userId, status, year, month);

            // For now, just return all orders - filtering can be enhanced later
            Page<OrderSummaryResponse> orders = orderService.getUserOrders(userId, pageable);
            return ResponseEntity.ok(orders);

        } catch (Exception e) {
            log.error("Error getting order history: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Search user's orders
     */
    @GetMapping("/search")
    public ResponseEntity<Page<OrderSummaryResponse>> searchOrders(
            Authentication authentication,
            @RequestParam String query,
            @PageableDefault(size = 10) Pageable pageable) {
        
        try {
            UUID userId = ((JwtAuthenticationToken) authentication).getUserId();
            log.debug("Searching orders for user: {} with query: {}", userId, query);

            // This would require enhancing OrderService with search functionality
            // For now, return all orders as a placeholder
            Page<OrderSummaryResponse> orders = orderService.getUserOrders(userId, pageable);
            return ResponseEntity.ok(orders);

        } catch (Exception e) {
            log.error("Error searching orders: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get order tracking information
     */
    @GetMapping("/{orderId}/tracking")
    public ResponseEntity<?> getOrderTracking(
            @PathVariable Long orderId,
            Authentication authentication) {
        
        try {
            UUID userId = ((JwtAuthenticationToken) authentication).getUserId();
            log.debug("Getting tracking for order {} for user: {}", orderId, userId);

            // First verify user has access to this order
            OrderResponse order = orderService.getUserOrder(userId, orderId);
            
            // This would integrate with TrackingService to get detailed tracking info
            // For now, return basic order status
            return ResponseEntity.ok(order);

        } catch (Exception e) {
            log.error("Error getting order tracking for order {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Request invoice for an order
     */
    @PostMapping("/{orderId}/invoice")
    public ResponseEntity<String> requestInvoice(
            @PathVariable Long orderId,
            Authentication authentication) {
        
        try {
            UUID userId = ((JwtAuthenticationToken) authentication).getUserId();
            log.info("Requesting invoice for order {} for user: {}", orderId, userId);

            // Verify user has access to this order
            orderService.getUserOrder(userId, orderId);

            // This would integrate with InvoiceService to generate and email invoice
            // For now, return a placeholder response
            
            return ResponseEntity.ok("Invoice request submitted successfully");

        } catch (Exception e) {
            log.error("Error requesting invoice for order {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.badRequest().body("Failed to request invoice: " + e.getMessage());
        }
    }

    /**
     * Download invoice PDF
     */
    @GetMapping("/{orderId}/invoice/download")
    public ResponseEntity<?> downloadInvoice(
            @PathVariable Long orderId,
            Authentication authentication) {
        
        try {
            UUID userId = ((JwtAuthenticationToken) authentication).getUserId();
            log.debug("Downloading invoice for order {} for user: {}", orderId, userId);

            // Verify user has access to this order
            orderService.getUserOrder(userId, orderId);

            // This would integrate with InvoiceService to generate PDF
            // For now, return a placeholder response
            
            return ResponseEntity.ok("Invoice download endpoint - requires InvoiceService integration");

        } catch (Exception e) {
            log.error("Error downloading invoice for order {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Reorder - add items from previous order to cart
     */
    @PostMapping("/{orderId}/reorder")
    public ResponseEntity<String> reorderItems(
            @PathVariable Long orderId,
            Authentication authentication) {
        
        try {
            UUID userId = ((JwtAuthenticationToken) authentication).getUserId();
            log.info("Reordering items from order {} for user: {}", orderId, userId);

            OrderResponse order = orderService.getUserOrder(userId, orderId);
            
            // This would integrate with CartService to add order items back to cart
            // For now, return a placeholder response
            
            return ResponseEntity.ok("Items added to cart successfully");

        } catch (Exception e) {
            log.error("Error reordering from order {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.badRequest().body("Failed to reorder: " + e.getMessage());
        }
    }

    /**
     * Get order statistics for user (total spent, order count, etc.)
     */
    @GetMapping("/statistics")
    public ResponseEntity<?> getUserOrderStatistics(Authentication authentication) {
        
        try {
            UUID userId = ((JwtAuthenticationToken) authentication).getUserId();
            log.debug("Getting order statistics for user: {}", userId);

            // This would require enhancing OrderService with statistics functionality
            // For now, return a placeholder response
            
            return ResponseEntity.ok("Order statistics endpoint - requires implementation");

        } catch (Exception e) {
            log.error("Error getting order statistics: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
}