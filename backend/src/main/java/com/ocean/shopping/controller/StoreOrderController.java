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
 * Controller for store owner order management
 */
@RestController
@RequestMapping("/api/store/{storeId}/orders")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('STORE_OWNER')")
public class StoreOrderController {

    private final OrderManagementService orderManagementService;

    /**
     * Get orders for a specific store
     */
    @GetMapping
    public ResponseEntity<Page<OrderSummaryResponse>> getStoreOrders(
            @PathVariable UUID storeId,
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime endDate,
            Authentication authentication) {
        
        try {
            UUID userId = ((JwtAuthenticationToken) authentication).getUserId();
            log.debug("Getting orders for store {} by user: {}", storeId, userId);

            Page<OrderSummaryResponse> orders = orderManagementService.getStoreOrders(
                storeId, pageable, status, startDate, endDate);

            return ResponseEntity.ok(orders);

        } catch (Exception e) {
            log.error("Error getting store orders: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Search orders for a store
     */
    @GetMapping("/search")
    public ResponseEntity<Page<OrderSummaryResponse>> searchStoreOrders(
            @PathVariable UUID storeId,
            @RequestParam String query,
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication) {
        
        try {
            UUID userId = ((JwtAuthenticationToken) authentication).getUserId();
            log.debug("Searching orders for store {} with query: {} by user: {}", storeId, query, userId);

            Page<OrderSummaryResponse> orders = orderManagementService.searchStoreOrders(storeId, query, pageable);
            return ResponseEntity.ok(orders);

        } catch (Exception e) {
            log.error("Error searching store orders: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get specific order details for store
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getStoreOrder(
            @PathVariable UUID storeId,
            @PathVariable UUID orderId,
            Authentication authentication) {
        
        try {
            UUID userId = ((JwtAuthenticationToken) authentication).getUserId();
            log.debug("Getting order {} for store {} by user: {}", orderId, storeId, userId);

            OrderResponse order = orderManagementService.getStoreOrder(storeId, orderId);
            return ResponseEntity.ok(order);

        } catch (Exception e) {
            log.error("Error getting store order: {}", e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get orders requiring attention for store
     */
    @GetMapping("/attention")
    public ResponseEntity<List<OrderSummaryResponse>> getOrdersRequiringAttention(
            @PathVariable UUID storeId,
            Authentication authentication) {
        
        try {
            UUID userId = ((JwtAuthenticationToken) authentication).getUserId();
            log.debug("Getting orders requiring attention for store {} by user: {}", storeId, userId);

            List<OrderSummaryResponse> orders = orderManagementService.getStoreOrdersRequiringAttention(storeId);
            return ResponseEntity.ok(orders);

        } catch (Exception e) {
            log.error("Error getting orders requiring attention: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update order status
     */
    @PutMapping("/{orderId}/status")
    public ResponseEntity<String> updateOrderStatus(
            @PathVariable UUID storeId,
            @PathVariable UUID orderId,
            @Valid @RequestBody OrderStatusUpdateRequest request,
            Authentication authentication) {
        
        try {
            UUID userId = ((JwtAuthenticationToken) authentication).getUserId();
            log.info("Updating order {} status to {} for store {} by user: {}", 
                orderId, request.getStatus(), storeId, userId);

            // First verify order belongs to store
            orderManagementService.getStoreOrder(storeId, orderId);
            
            // Update status
            orderManagementService.updateOrderStatus(orderId, request, userId);

            return ResponseEntity.ok("Order status updated successfully");

        } catch (Exception e) {
            log.error("Error updating order status: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Failed to update order status: " + e.getMessage());
        }
    }

    /**
     * Process refund for an order
     */
    @PostMapping("/{orderId}/refund")
    public ResponseEntity<String> processRefund(
            @PathVariable UUID storeId,
            @PathVariable UUID orderId,
            @RequestParam BigDecimal amount,
            @RequestParam String reason,
            Authentication authentication) {
        
        try {
            UUID userId = ((JwtAuthenticationToken) authentication).getUserId();
            log.info("Processing refund of {} for order {} in store {} by user: {}", 
                amount, orderId, storeId, userId);

            // First verify order belongs to store
            orderManagementService.getStoreOrder(storeId, orderId);
            
            // Process refund
            orderManagementService.processRefund(orderId, amount, reason, userId);

            return ResponseEntity.ok("Refund processed successfully");

        } catch (Exception e) {
            log.error("Error processing refund: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Failed to process refund: " + e.getMessage());
        }
    }

    /**
     * Get store revenue analytics
     */
    @GetMapping("/analytics/revenue")
    public ResponseEntity<Map<String, Object>> getRevenueAnalytics(
            @PathVariable UUID storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime endDate,
            Authentication authentication) {
        
        try {
            UUID userId = ((JwtAuthenticationToken) authentication).getUserId();
            log.debug("Getting revenue analytics for store {} from {} to {} by user: {}", 
                storeId, startDate, endDate, userId);

            Map<String, Object> analytics = orderManagementService.getStoreRevenueAnalytics(
                storeId, startDate, endDate);

            return ResponseEntity.ok(analytics);

        } catch (Exception e) {
            log.error("Error getting revenue analytics: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get order status summary for store
     */
    @GetMapping("/analytics/status-summary")
    public ResponseEntity<?> getOrderStatusSummary(
            @PathVariable UUID storeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime endDate,
            Authentication authentication) {
        
        try {
            UUID userId = ((JwtAuthenticationToken) authentication).getUserId();
            log.debug("Getting order status summary for store {} by user: {}", storeId, userId);

            // This would require additional implementation in OrderManagementService
            // For now, return placeholder
            return ResponseEntity.ok("Order status summary endpoint - requires implementation");

        } catch (Exception e) {
            log.error("Error getting order status summary: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Export orders to CSV
     */
    @GetMapping("/export")
    public ResponseEntity<?> exportOrders(
            @PathVariable UUID storeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime endDate,
            @RequestParam(required = false) OrderStatus status,
            Authentication authentication) {
        
        try {
            UUID userId = ((JwtAuthenticationToken) authentication).getUserId();
            log.info("Exporting orders for store {} by user: {}", storeId, userId);

            // This would require implementation of export functionality
            // For now, return placeholder
            return ResponseEntity.ok("Order export endpoint - requires implementation");

        } catch (Exception e) {
            log.error("Error exporting orders: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Bulk update order statuses
     */
    @PutMapping("/bulk/status")
    public ResponseEntity<String> bulkUpdateOrderStatus(
            @PathVariable UUID storeId,
            @RequestParam List<Long> orderIds,
            @RequestParam OrderStatus status,
            @RequestParam(required = false) String notes,
            Authentication authentication) {
        
        try {
            UUID userId = ((JwtAuthenticationToken) authentication).getUserId();
            log.info("Bulk updating {} orders to status {} for store {} by user: {}", 
                orderIds.size(), status, storeId, userId);

            // This would require implementation of bulk operations
            // For now, return placeholder
            return ResponseEntity.ok("Bulk status update endpoint - requires implementation");

        } catch (Exception e) {
            log.error("Error bulk updating order status: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Failed to bulk update orders: " + e.getMessage());
        }
    }

    /**
     * Get pending shipments for store
     */
    @GetMapping("/shipments/pending")
    public ResponseEntity<List<OrderSummaryResponse>> getPendingShipments(
            @PathVariable UUID storeId,
            Authentication authentication) {
        
        try {
            UUID userId = ((JwtAuthenticationToken) authentication).getUserId();
            log.debug("Getting pending shipments for store {} by user: {}", storeId, userId);

            // Get orders that are confirmed or processing (ready to ship)
            List<OrderStatus> shipmentReadyStatuses = List.of(OrderStatus.CONFIRMED, OrderStatus.PROCESSING);
            // This would require filtering by status in the service
            List<OrderSummaryResponse> orders = orderManagementService.getStoreOrdersRequiringAttention(storeId);
            
            return ResponseEntity.ok(orders);

        } catch (Exception e) {
            log.error("Error getting pending shipments: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
}