package com.ocean.shopping.service;

import com.ocean.shopping.model.entity.*;
import com.ocean.shopping.model.entity.enums.OrderStatus;
import com.ocean.shopping.model.entity.enums.PaymentProvider;
import com.ocean.shopping.model.entity.enums.PaymentStatus;
import com.ocean.shopping.dto.order.*;
import com.ocean.shopping.repository.OrderRepository;
import com.ocean.shopping.repository.StoreRepository;
import com.ocean.shopping.service.payment.PaymentProviderService;
import com.ocean.shopping.service.lock.DistributedLockManager;
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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Enhanced OrderService with checkout processing integration
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final StoreRepository storeRepository;
    private final CartService cartService;
    private final PaymentService paymentService;
    private final UserService userService;
    private final NotificationService notificationService;
    private final ShippingService shippingService;
    private final DistributedLockManager lockManager;
    
    // Order number generation prefix
    private static final String ORDER_NUMBER_PREFIX = "OSC";

    /**
     * Process checkout and create order with distributed lock protection
     */
    @Transactional
    public CheckoutResponse processCheckout(CheckoutRequest request, UUID userId, String sessionId) {
        log.info("Processing checkout for user: {} or session: {}", userId, sessionId);

        String userIdentifier = userId != null ? userId.toString() : sessionId;
        String cartLockKey = lockManager.cartLockKey(userIdentifier);
        
        // Execute checkout with distributed lock protection
        return lockManager.executeWithLockOrThrow(cartLockKey, () -> {
            return processCheckoutInternal(request, userId, sessionId);
        });
    }

    /**
     * Internal checkout processing method (protected by locks)
     */
    private CheckoutResponse processCheckoutInternal(CheckoutRequest request, UUID userId, String sessionId) {
        try {
            // Get cart
            Cart cart = getCartForCheckout(userId, sessionId);
            if (cart == null || cart.getCartItems().isEmpty()) {
                throw new BadRequestException("Cart is empty");
            }

            // Collect all inventory locks needed for this order
            List<String> inventoryLockKeys = cart.getCartItems().stream()
                    .map(item -> lockManager.inventoryLockKey(item.getProduct().getId().toString()))
                    .collect(Collectors.toList());

            // Execute with multiple inventory locks
            return executeWithInventoryLocks(inventoryLockKeys, () -> {
                // Validate cart items and update prices (within inventory locks)
                validateAndUpdateCart(cart);

                // Create order from cart
                Order order = createOrderFromCart(cart, request);

                // Process payment with lock protection
                return processPaymentAndFinalizeOrder(order, request, userId, sessionId, cart);
            });

        } catch (Exception e) {
            log.error("Error processing checkout for user: {} or session: {}", userId, sessionId, e);
            throw e;
        }
    }

    /**
     * Execute operation with multiple inventory locks
     */
    private CheckoutResponse executeWithInventoryLocks(List<String> lockKeys, Callable<CheckoutResponse> operation) {
        if (lockKeys.isEmpty()) {
            try {
                return operation.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        String firstLock = lockKeys.get(0);
        List<String> remainingLocks = lockKeys.subList(1, lockKeys.size());

        return lockManager.executeWithLockOrThrow(firstLock, () -> {
            return executeWithInventoryLocks(remainingLocks, operation);
        });
    }

    /**
     * Process payment and finalize order
     */
    private CheckoutResponse processPaymentAndFinalizeOrder(Order order, CheckoutRequest request, 
                                                           UUID userId, String sessionId, Cart cart) {
        // Generate unique payment lock key for this order
        String paymentLockKey = lockManager.paymentLockKey(order.getId().toString());
        
        try {
            return lockManager.executeWithLockOrThrow(paymentLockKey, () -> {
                // Process payment
                PaymentProviderService.PaymentIntent paymentIntent = 
                    paymentService.createPaymentIntent(order, PaymentProvider.STRIPE);

                // Confirm payment
                PaymentProviderService.PaymentResult paymentResult = 
                    paymentService.confirmPayment(paymentIntent.getId(), request.getPaymentMethodId());

                if (paymentResult.getStatus() != PaymentStatus.COMPLETED) {
                    throw new BadRequestException("Payment failed: " + paymentResult.getFailureReason());
                }

                // Update order status after successful payment
                order.setStatus(OrderStatus.CONFIRMED);
                order.setConfirmedAt(ZonedDateTime.now());
                order = orderRepository.save(order);

                // Clear cart after successful order
                if (userId != null) {
                    cartService.clearCart(userId);
                } else {
                    cartService.clearSessionCart(sessionId);
                }

                // Send confirmation email asynchronously
                CompletableFuture.runAsync(() -> sendOrderConfirmationEmail(order));

                // Create shipment if needed
                CompletableFuture.runAsync(() -> createShipmentForOrder(order));

                log.info("Checkout completed successfully for order: {}", order.getOrderNumber());

                return CheckoutResponse.success(
                    order.getId(),
                    order.getOrderNumber(),
                    order.getTotalAmount(),
                    order.getCurrency(),
                    paymentIntent.getId(),
                    order.getCustomerEmail()
                );
            });
        } catch (Exception e) {
            log.error("Checkout failed: {}", e.getMessage(), e);
            return CheckoutResponse.error("Checkout failed: " + e.getMessage());
        }
    }

    /**
     * Get user orders with pagination
     */
    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getUserOrders(UUID userId, Pageable pageable) {
        User user = userService.getUserById(userId);
        Page<Order> orders = orderRepository.findByUserOrderByCreatedAtDesc(user, pageable);
        return orders.map(this::convertToSummaryResponse);
    }

    /**
     * Get order by ID for user
     */
    @Transactional(readOnly = true)
    public OrderResponse getUserOrder(UUID userId, UUID orderId) {
        User user = userService.getUserById(userId);
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        
        if (!order.getUser().getId().equals(userId)) {
            throw new BadRequestException("Access denied to this order");
        }

        return convertToOrderResponse(order);
    }

    /**
     * Get order by order number for user
     */
    @Transactional(readOnly = true)
    public OrderResponse getUserOrderByNumber(UUID userId, String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        
        if (order.getUser() != null && !order.getUser().getId().equals(userId)) {
            throw new BadRequestException("Access denied to this order");
        }

        return convertToOrderResponse(order);
    }

    /**
     * Cancel order by user
     */
    @Transactional
    public void cancelOrder(UUID userId, UUID orderId, String reason) {
        User user = userService.getUserById(userId);
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        
        if (!order.getUser().getId().equals(userId)) {
            throw new BadRequestException("Access denied to this order");
        }

        if (!canCancelOrder(order)) {
            throw new BadRequestException("Order cannot be cancelled in its current state");
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(ZonedDateTime.now());
        order.setInternalNotes((order.getInternalNotes() != null ? order.getInternalNotes() + "\n" : "") 
            + "Cancelled by customer: " + reason);

        orderRepository.save(order);

        // Process refund if payment was completed
        CompletableFuture.runAsync(() -> processOrderRefund(order, "Customer cancellation"));

        // Send cancellation notification
        CompletableFuture.runAsync(() -> sendOrderCancellationEmail(order));

        log.info("Order {} cancelled by user {}", order.getOrderNumber(), userId);
    }

    /**
     * Update order status (for internal use)
     */
    @Transactional
    public void updateOrderStatus(UUID orderId, OrderStatus newStatus, String internalNotes) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(newStatus);
        
        // Update timestamps based on status
        ZonedDateTime now = ZonedDateTime.now();
        switch (newStatus) {
            case CONFIRMED -> order.setConfirmedAt(now);
            case SHIPPED -> order.setShippedAt(now);
            case DELIVERED -> order.setDeliveredAt(now);
            case CANCELLED -> order.setCancelledAt(now);
        }

        if (internalNotes != null) {
            order.setInternalNotes((order.getInternalNotes() != null ? order.getInternalNotes() + "\n" : "") 
                + internalNotes);
        }

        orderRepository.save(order);

        // Send status update notification
        CompletableFuture.runAsync(() -> sendOrderStatusUpdateEmail(order, oldStatus, newStatus));

        log.info("Order {} status updated from {} to {}", order.getOrderNumber(), oldStatus, newStatus);
    }

    // Private helper methods

    private Cart getCartForCheckout(UUID userId, String sessionId) {
        if (userId != null) {
            return cartService.getUserCart(userId);
        } else if (sessionId != null) {
            return cartService.getSessionCart(sessionId);
        }
        throw new BadRequestException("Either user ID or session ID must be provided");
    }

    private void validateAndUpdateCart(Cart cart) {
        // Validate cart items are still available and prices are current
        for (CartItem cartItem : cart.getCartItems()) {
            Product product = cartItem.getProduct();
            
            // Check product availability
            if (!product.isActive() || !product.isAvailable()) {
                throw new BadRequestException("Product " + product.getName() + " is no longer available");
            }
            
            // Check stock availability
            if (product.getStockQuantity() != null && product.getStockQuantity() < cartItem.getQuantity()) {
                throw new BadRequestException("Insufficient stock for product " + product.getName());
            }
            
            // Update price if changed (optional - could also keep locked prices)
            BigDecimal currentPrice = product.getPrice();
            if (!cartItem.getUnitPrice().equals(currentPrice)) {
                log.info("Price updated for product {} from {} to {}", 
                    product.getName(), cartItem.getUnitPrice(), currentPrice);
                cartItem.setUnitPrice(currentPrice);
            }
        }
    }

    private Order createOrderFromCart(Cart cart, CheckoutRequest request) {
        // Generate unique order number
        String orderNumber = generateOrderNumber();

        // Calculate totals
        BigDecimal subtotal = calculateSubtotal(cart.getCartItems());
        BigDecimal taxAmount = calculateTax(subtotal, request.getBillingAddress().getCountry());
        BigDecimal shippingAmount = calculateShipping(cart, request.getShippingAddress());
        BigDecimal discountAmount = BigDecimal.ZERO; // TODO: Apply coupon if provided
        BigDecimal totalAmount = subtotal.add(taxAmount).add(shippingAmount).subtract(discountAmount);

        // Create order
        Order order = Order.builder()
            .orderNumber(orderNumber)
            .user(cart.getUser())
            .store(determineStoreFromCart(cart))
            .status(OrderStatus.PENDING)
            .customerEmail(request.getCustomerEmail())
            .customerPhone(request.getCustomerPhone())
            .billingFirstName(request.getBillingAddress().getFirstName())
            .billingLastName(request.getBillingAddress().getLastName())
            .billingAddressLine1(request.getBillingAddress().getAddressLine1())
            .billingCity(request.getBillingAddress().getCity())
            .billingPostalCode(request.getBillingAddress().getPostalCode())
            .billingCountry(request.getBillingAddress().getCountry())
            .shippingFirstName(request.getShippingAddress().getFirstName())
            .shippingLastName(request.getShippingAddress().getLastName())
            .shippingAddressLine1(request.getShippingAddress().getAddressLine1())
            .shippingCity(request.getShippingAddress().getCity())
            .shippingPostalCode(request.getShippingAddress().getPostalCode())
            .shippingCountry(request.getShippingAddress().getCountry())
            .subtotal(subtotal)
            .taxAmount(taxAmount)
            .shippingAmount(shippingAmount)
            .discountAmount(discountAmount)
            .totalAmount(totalAmount)
            .notes(request.getNotes())
            .build();

        order = orderRepository.save(order);

        // Create order items
        createOrderItems(order, cart.getCartItems());

        return order;
    }

    private void createOrderItems(Order order, List<CartItem> cartItems) {
        List<OrderItem> orderItems = cartItems.stream()
            .map(cartItem -> OrderItem.builder()
                .order(order)
                .product(cartItem.getProduct())
                .productVariant(cartItem.getProductVariant())
                .quantity(cartItem.getQuantity())
                .unitPrice(cartItem.getUnitPrice())
                .totalPrice(cartItem.getUnitPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())))
                .currency("USD")
                .build())
            .collect(Collectors.toList());

        order.setOrderItems(orderItems);
        orderRepository.save(order);
    }

    private String generateOrderNumber() {
        return ORDER_NUMBER_PREFIX + "-" + System.currentTimeMillis();
    }

    private BigDecimal calculateSubtotal(List<CartItem> cartItems) {
        return cartItems.stream()
            .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateTax(BigDecimal subtotal, String country) {
        // Simple tax calculation - should be enhanced with actual tax rules
        if ("US".equals(country)) {
            return subtotal.multiply(BigDecimal.valueOf(0.08)); // 8% tax
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal calculateShipping(Cart cart, CheckoutRequest.AddressDto address) {
        // Use existing shipping service
        return shippingService.calculateShippingCost(cart.getId(), address.getCountry());
    }

    private Store determineStoreFromCart(Cart cart) {
        // For now, assume single store. In marketplace, this would be more complex
        if (!cart.getCartItems().isEmpty()) {
            return cart.getCartItems().get(0).getProduct().getStore();
        }
        throw new BadRequestException("Cannot determine store from empty cart");
    }

    private boolean canCancelOrder(Order order) {
        return order.getStatus() == OrderStatus.PENDING || 
               order.getStatus() == OrderStatus.CONFIRMED;
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
            .requiresAttention(order.getStatus() == OrderStatus.PENDING || order.getStatus() == OrderStatus.CONFIRMED)
            .statusDisplayText(getStatusDisplayText(order.getStatus()))
            .shippingCity(order.getShippingCity())
            .shippingCountry(order.getShippingCountry())
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

    // Async notification methods
    private void sendOrderConfirmationEmail(Order order) {
        try {
            notificationService.sendOrderConfirmationEmail(order.getCustomerEmail(), order);
        } catch (Exception e) {
            log.error("Failed to send order confirmation email for order {}: {}", order.getOrderNumber(), e.getMessage());
        }
    }

    private void sendOrderStatusUpdateEmail(Order order, OrderStatus oldStatus, OrderStatus newStatus) {
        try {
            notificationService.sendOrderStatusUpdateEmail(order.getCustomerEmail(), order, oldStatus, newStatus);
        } catch (Exception e) {
            log.error("Failed to send order status update email for order {}: {}", order.getOrderNumber(), e.getMessage());
        }
    }

    private void sendOrderCancellationEmail(Order order) {
        try {
            notificationService.sendOrderCancellationEmail(order.getCustomerEmail(), order);
        } catch (Exception e) {
            log.error("Failed to send order cancellation email for order {}: {}", order.getOrderNumber(), e.getMessage());
        }
    }

    private void createShipmentForOrder(Order order) {
        try {
            shippingService.createShipmentForOrder(order.getId());
        } catch (Exception e) {
            log.error("Failed to create shipment for order {}: {}", order.getOrderNumber(), e.getMessage());
        }
    }

    private void processOrderRefund(Order order, String reason) {
        try {
            paymentService.processRefund(order.getId(), order.getTotalAmount(), reason);
        } catch (Exception e) {
            log.error("Failed to process refund for order {}: {}", order.getOrderNumber(), e.getMessage());
        }
    }
}