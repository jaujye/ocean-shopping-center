package com.ocean.shopping.service;

import com.ocean.shopping.model.entity.*;
import com.ocean.shopping.model.entity.enums.OrderStatus;
import com.ocean.shopping.dto.order.*;
import com.ocean.shopping.repository.OrderRepository;
import com.ocean.shopping.repository.StoreRepository;
import com.ocean.shopping.exception.BadRequestException;
import com.ocean.shopping.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Service for order management by admins and store owners
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderManagementService {

    private final OrderRepository orderRepository;
    private final StoreRepository storeRepository;
    private final OrderService orderService;
    private final UserService userService;
    private final NotificationService notificationService;
    private final PaymentService paymentService;
    private final TrackingService trackingService;

    // Admin Operations

    /**
     * Get all orders with pagination and filtering
     */
    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getAllOrders(Pageable pageable, OrderStatus status, 
                                                  ZonedDateTime startDate, ZonedDateTime endDate) {
        Page<Order> orders;
        
        if (status != null && startDate != null && endDate != null) {
            orders = orderRepository.findByStatusAndDateRange(status, startDate, endDate, pageable);
        } else if (status != null) {
            orders = orderRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        } else if (startDate != null && endDate != null) {
            orders = orderRepository.findByDateRange(startDate, endDate, pageable);
        } else {
            orders = orderRepository.findAll(pageable);
        }
        
        return orders.map(this::convertToSummaryResponse);
    }

    /**
     * Search orders across all stores
     */
    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> searchAllOrders(String searchTerm, Pageable pageable) {
        Page<Order> orders = orderRepository.searchOrders(searchTerm, pageable);
        return orders.map(this::convertToSummaryResponse);
    }

    /**
     * Get order details for admin
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrderForAdmin(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return convertToOrderResponse(order);
    }

    /**
     * Get orders requiring attention
     */
    @Transactional(readOnly = true)
    public List<OrderSummaryResponse> getOrdersRequiringAttention() {
        List<Order> orders = orderRepository.findOrdersRequiringAttention();
        return orders.stream()
            .map(this::convertToSummaryResponse)
            .collect(Collectors.toList());
    }

    /**
     * Get revenue analytics for admin
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getRevenueAnalytics(ZonedDateTime startDate, ZonedDateTime endDate) {
        List<OrderStatus> completedStatuses = List.of(OrderStatus.CONFIRMED, OrderStatus.PROCESSING, 
                                                     OrderStatus.SHIPPED, OrderStatus.DELIVERED);

        // Total revenue
        BigDecimal totalRevenue = orderRepository.sumTotalAmountByDateRangeAndStatuses(
            startDate, endDate, completedStatuses);

        // Order count
        long totalOrders = orderRepository.countByDateRangeAndStatuses(startDate, endDate, completedStatuses);

        // Average order value
        BigDecimal averageOrderValue = totalOrders > 0 ? 
            totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, BigDecimal.ROUND_HALF_UP) : 
            BigDecimal.ZERO;

        // Top performing stores
        List<Object[]> topStores = orderRepository.getTopStoresByRevenue(completedStatuses, startDate, endDate);

        Map<String, Object> analytics = new HashMap<>();
        analytics.put("totalRevenue", totalRevenue);
        analytics.put("totalOrders", totalOrders);
        analytics.put("averageOrderValue", averageOrderValue);
        analytics.put("topStores", topStores);
        analytics.put("period", Map.of("start", startDate, "end", endDate));

        return analytics;
    }

    // Store Operations

    /**
     * Get orders for a specific store
     */
    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getStoreOrders(Long storeId, Pageable pageable, 
                                                    OrderStatus status, ZonedDateTime startDate, ZonedDateTime endDate) {
        Store store = storeRepository.findById(storeId)
            .orElseThrow(() -> new ResourceNotFoundException("Store not found"));

        Page<Order> orders;
        
        if (status != null && startDate != null && endDate != null) {
            orders = orderRepository.findByStoreAndStatusAndDateRange(store, status, startDate, endDate, pageable);
        } else if (status != null) {
            orders = orderRepository.findByStoreAndStatusOrderByCreatedAtDesc(store, status, pageable);
        } else if (startDate != null && endDate != null) {
            orders = orderRepository.findByStoreAndDateRange(store, startDate, endDate, pageable);
        } else {
            orders = orderRepository.findByStoreOrderByCreatedAtDesc(store, pageable);
        }
        
        return orders.map(this::convertToSummaryResponse);
    }

    /**
     * Search orders for a specific store
     */
    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> searchStoreOrders(Long storeId, String searchTerm, Pageable pageable) {
        Store store = storeRepository.findById(storeId)
            .orElseThrow(() -> new ResourceNotFoundException("Store not found"));
            
        Page<Order> orders = orderRepository.searchOrdersByStore(store, searchTerm, pageable);
        return orders.map(this::convertToSummaryResponse);
    }

    /**
     * Get order details for store owner
     */
    @Transactional(readOnly = true)
    public OrderResponse getStoreOrder(Long storeId, Long orderId) {
        Store store = storeRepository.findById(storeId)
            .orElseThrow(() -> new ResourceNotFoundException("Store not found"));
            
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getStore().getId().equals(storeId)) {
            throw new BadRequestException("Order does not belong to this store");
        }

        return convertToOrderResponse(order);
    }

    /**
     * Get orders requiring attention for a store
     */
    @Transactional(readOnly = true)
    public List<OrderSummaryResponse> getStoreOrdersRequiringAttention(Long storeId) {
        Store store = storeRepository.findById(storeId)
            .orElseThrow(() -> new ResourceNotFoundException("Store not found"));
            
        List<Order> orders = orderRepository.findOrdersRequiringAttentionByStore(store);
        return orders.stream()
            .map(this::convertToSummaryResponse)
            .collect(Collectors.toList());
    }

    /**
     * Update order status
     */
    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatusUpdateRequest request, UUID updatedBy) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        validateStatusTransition(order.getStatus(), request.getStatus());

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(request.getStatus());

        // Update timestamps based on status
        ZonedDateTime now = ZonedDateTime.now();
        switch (request.getStatus()) {
            case CONFIRMED -> order.setConfirmedAt(now);
            case SHIPPED -> {
                order.setShippedAt(now);
                if (request.getTrackingNumber() != null) {
                    updateTrackingNumber(order, request.getTrackingNumber());
                }
            }
            case DELIVERED -> order.setDeliveredAt(now);
            case CANCELLED -> order.setCancelledAt(now);
        }

        // Add internal notes
        if (request.getInternalNotes() != null) {
            String noteEntry = String.format("[%s] %s - %s", 
                now.truncatedTo(ChronoUnit.MINUTES), 
                getUserDisplayName(updatedBy), 
                request.getInternalNotes());
            
            order.setInternalNotes((order.getInternalNotes() != null ? order.getInternalNotes() + "\n" : "") + noteEntry);
        }

        orderRepository.save(order);

        // Send notification to customer if requested
        if (request.isNotifyCustomer()) {
            CompletableFuture.runAsync(() -> sendStatusUpdateNotification(order, oldStatus, request.getStatus()));
        }

        // Handle status-specific actions
        handleStatusSpecificActions(order, oldStatus, request.getStatus());

        log.info("Order {} status updated from {} to {} by user {}", 
            order.getOrderNumber(), oldStatus, request.getStatus(), updatedBy);
    }

    /**
     * Process refund for an order
     */
    @Transactional
    public void processRefund(Long orderId, BigDecimal amount, String reason, UUID processedBy) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (amount.compareTo(order.getTotalAmount()) > 0) {
            throw new BadRequestException("Refund amount cannot exceed order total");
        }

        // Process refund through payment service
        paymentService.processRefund(orderId, amount, reason);

        // Add internal note about refund
        String noteEntry = String.format("[%s] Refund processed: %s %s - %s", 
            ZonedDateTime.now().truncatedTo(ChronoUnit.MINUTES), 
            amount, order.getCurrency(), reason);
        
        order.setInternalNotes((order.getInternalNotes() != null ? order.getInternalNotes() + "\n" : "") + noteEntry);

        // Update status if full refund
        if (amount.equals(order.getTotalAmount()) && order.getStatus() != OrderStatus.RETURNED) {
            order.setStatus(OrderStatus.CANCELLED);
            order.setCancelledAt(ZonedDateTime.now());
        }

        orderRepository.save(order);

        // Send refund notification
        CompletableFuture.runAsync(() -> sendRefundNotification(order, amount, reason));

        log.info("Refund of {} processed for order {} by user {}", amount, order.getOrderNumber(), processedBy);
    }

    /**
     * Get store revenue analytics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getStoreRevenueAnalytics(Long storeId, ZonedDateTime startDate, ZonedDateTime endDate) {
        Store store = storeRepository.findById(storeId)
            .orElseThrow(() -> new ResourceNotFoundException("Store not found"));

        List<OrderStatus> completedStatuses = List.of(OrderStatus.CONFIRMED, OrderStatus.PROCESSING, 
                                                     OrderStatus.SHIPPED, OrderStatus.DELIVERED);

        // Total revenue for store
        BigDecimal totalRevenue = orderRepository.sumTotalAmountByStoreAndDateRangeAndStatuses(
            store, startDate, endDate, completedStatuses);

        // Order count for store
        long totalOrders = orderRepository.countByStoreAndDateRange(store, startDate, endDate);

        // Average order value
        BigDecimal averageOrderValue = totalOrders > 0 ? 
            totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, BigDecimal.ROUND_HALF_UP) : 
            BigDecimal.ZERO;

        // Daily revenue breakdown
        List<Object[]> dailyRevenue = orderRepository.getDailyRevenue(store, completedStatuses, startDate, endDate);

        // Top customers
        List<Object[]> topCustomers = orderRepository.findTopCustomersByOrderCount(store, Pageable.ofSize(10));

        Map<String, Object> analytics = new HashMap<>();
        analytics.put("storeId", storeId);
        analytics.put("storeName", store.getName());
        analytics.put("totalRevenue", totalRevenue);
        analytics.put("totalOrders", totalOrders);
        analytics.put("averageOrderValue", averageOrderValue);
        analytics.put("dailyRevenue", dailyRevenue);
        analytics.put("topCustomers", topCustomers);
        analytics.put("period", Map.of("start", startDate, "end", endDate));

        return analytics;
    }

    // Private helper methods

    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        // Define valid status transitions
        Map<OrderStatus, List<OrderStatus>> validTransitions = Map.of(
            OrderStatus.PENDING, List.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
            OrderStatus.CONFIRMED, List.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED),
            OrderStatus.PROCESSING, List.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED),
            OrderStatus.SHIPPED, List.of(OrderStatus.DELIVERED, OrderStatus.RETURNED),
            OrderStatus.DELIVERED, List.of(OrderStatus.RETURNED),
            OrderStatus.CANCELLED, List.of(), // No transitions from cancelled
            OrderStatus.RETURNED, List.of()  // No transitions from returned
        );

        List<OrderStatus> validNextStatuses = validTransitions.get(currentStatus);
        if (validNextStatuses == null || !validNextStatuses.contains(newStatus)) {
            throw new BadRequestException(
                String.format("Invalid status transition from %s to %s", currentStatus, newStatus));
        }
    }

    private void handleStatusSpecificActions(Order order, OrderStatus oldStatus, OrderStatus newStatus) {
        switch (newStatus) {
            case CONFIRMED -> {
                // Create shipment preparation
                CompletableFuture.runAsync(() -> prepareShipment(order));
            }
            case SHIPPED -> {
                // Start tracking
                CompletableFuture.runAsync(() -> initializeTracking(order));
            }
            case CANCELLED -> {
                if (oldStatus == OrderStatus.CONFIRMED || oldStatus == OrderStatus.PROCESSING) {
                    // Process automatic refund for confirmed orders
                    CompletableFuture.runAsync(() -> processAutomaticRefund(order));
                }
            }
        }
    }

    private void updateTrackingNumber(Order order, String trackingNumber) {
        // Update tracking in the tracking service
        trackingService.updateOrderTracking(order.getId(), trackingNumber);
    }

    private String getUserDisplayName(UUID userId) {
        try {
            User user = userService.getUserById(userId);
            return user.getFirstName() + " " + user.getLastName();
        } catch (Exception e) {
            return "System";
        }
    }

    // Conversion methods
    private OrderSummaryResponse convertToSummaryResponse(Order order) {
        return OrderSummaryResponse.builder()
            .id(order.getId())
            .orderNumber(order.getOrderNumber())
            .status(order.getStatus())
            .customerEmail(order.getCustomerEmail())
            .customerName(order.getBillingFullName())
            .totalAmount(order.getTotalAmount())
            .currency(order.getCurrency())
            .itemCount(order.getOrderItems() != null ? order.getOrderItems().size() : 0)
            .storeId(order.getStore().getId())
            .storeName(order.getStore().getName())
            .createdAt(order.getCreatedAt())
            .updatedAt(order.getUpdatedAt())
            .requiresAttention(requiresAttention(order))
            .statusDisplayText(getStatusDisplayText(order.getStatus()))
            .shippingCity(order.getShippingCity())
            .shippingCountry(order.getShippingCountry())
            .build();
    }

    private OrderResponse convertToOrderResponse(Order order) {
        return OrderResponse.builder()
            .id(order.getId())
            .orderNumber(order.getOrderNumber())
            .status(order.getStatus())
            .customerEmail(order.getCustomerEmail())
            .customerPhone(order.getCustomerPhone())
            .billingFirstName(order.getBillingFirstName())
            .billingLastName(order.getBillingLastName())
            .billingAddressLine1(order.getBillingAddressLine1())
            .billingCity(order.getBillingCity())
            .billingPostalCode(order.getBillingPostalCode())
            .billingCountry(order.getBillingCountry())
            .shippingFirstName(order.getShippingFirstName())
            .shippingLastName(order.getShippingLastName())
            .shippingAddressLine1(order.getShippingAddressLine1())
            .shippingCity(order.getShippingCity())
            .shippingPostalCode(order.getShippingPostalCode())
            .shippingCountry(order.getShippingCountry())
            .subtotal(order.getSubtotal())
            .taxAmount(order.getTaxAmount())
            .shippingAmount(order.getShippingAmount())
            .discountAmount(order.getDiscountAmount())
            .totalAmount(order.getTotalAmount())
            .currency(order.getCurrency())
            .orderItems(convertToOrderItemResponses(order.getOrderItems()))
            .storeId(order.getStore().getId())
            .storeName(order.getStore().getName())
            .createdAt(order.getCreatedAt())
            .updatedAt(order.getUpdatedAt())
            .confirmedAt(order.getConfirmedAt())
            .shippedAt(order.getShippedAt())
            .deliveredAt(order.getDeliveredAt())
            .cancelledAt(order.getCancelledAt())
            .notes(order.getNotes())
            .internalNotes(order.getInternalNotes())
            .build();
    }

    private List<OrderItemResponse> convertToOrderItemResponses(List<OrderItem> orderItems) {
        if (orderItems == null) return List.of();
        
        return orderItems.stream()
            .map(item -> OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .productSku(item.getProduct().getSku())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getTotalPrice())
                .currency(item.getCurrency())
                .variantName(item.getProductVariant() != null ? item.getProductVariant().getName() : null)
                .variantSku(item.getProductVariant() != null ? item.getProductVariant().getSku() : null)
                .storeId(item.getProduct().getStore().getId())
                .storeName(item.getProduct().getStore().getName())
                .build())
            .collect(Collectors.toList());
    }

    private boolean requiresAttention(Order order) {
        // Orders older than 24 hours in pending status require attention
        if (order.getStatus() == OrderStatus.PENDING) {
            ZonedDateTime cutoff = ZonedDateTime.now().minusHours(24);
            return order.getCreatedAt().isBefore(cutoff);
        }
        
        // Orders older than 48 hours in confirmed status require attention
        if (order.getStatus() == OrderStatus.CONFIRMED) {
            ZonedDateTime cutoff = ZonedDateTime.now().minusHours(48);
            return order.getConfirmedAt() != null && order.getConfirmedAt().isBefore(cutoff);
        }
        
        return false;
    }

    private String getStatusDisplayText(OrderStatus status) {
        return switch (status) {
            case PENDING -> "Awaiting Payment";
            case CONFIRMED -> "Order Confirmed";
            case PROCESSING -> "Being Prepared";
            case SHIPPED -> "Shipped";
            case DELIVERED -> "Delivered";
            case CANCELLED -> "Cancelled";
            case RETURNED -> "Returned";
        };
    }

    // Async operations
    private void sendStatusUpdateNotification(Order order, OrderStatus oldStatus, OrderStatus newStatus) {
        try {
            notificationService.sendOrderStatusUpdateEmail(order.getCustomerEmail(), order, oldStatus, newStatus);
        } catch (Exception e) {
            log.error("Failed to send status update notification for order {}: {}", order.getOrderNumber(), e.getMessage());
        }
    }

    private void sendRefundNotification(Order order, BigDecimal amount, String reason) {
        try {
            notificationService.sendRefundNotificationEmail(order.getCustomerEmail(), order, amount, reason);
        } catch (Exception e) {
            log.error("Failed to send refund notification for order {}: {}", order.getOrderNumber(), e.getMessage());
        }
    }

    private void prepareShipment(Order order) {
        try {
            // Integration with shipping service to prepare shipment
            log.info("Preparing shipment for order {}", order.getOrderNumber());
        } catch (Exception e) {
            log.error("Failed to prepare shipment for order {}: {}", order.getOrderNumber(), e.getMessage());
        }
    }

    private void initializeTracking(Order order) {
        try {
            trackingService.initializeOrderTracking(order.getId());
        } catch (Exception e) {
            log.error("Failed to initialize tracking for order {}: {}", order.getOrderNumber(), e.getMessage());
        }
    }

    private void processAutomaticRefund(Order order) {
        try {
            paymentService.processRefund(order.getId(), order.getTotalAmount(), "Order cancellation");
        } catch (Exception e) {
            log.error("Failed to process automatic refund for order {}: {}", order.getOrderNumber(), e.getMessage());
        }
    }
}