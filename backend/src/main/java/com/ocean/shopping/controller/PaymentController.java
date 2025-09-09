package com.ocean.shopping.controller;

import com.ocean.shopping.dto.payment.*;
import com.ocean.shopping.exception.ErrorResponse;
import com.ocean.shopping.model.entity.Order;
import com.ocean.shopping.model.entity.Payment;
import com.ocean.shopping.model.entity.PaymentMethod;
import com.ocean.shopping.model.entity.User;
import com.ocean.shopping.repository.OrderRepository;
import com.ocean.shopping.service.PaymentService;
import com.ocean.shopping.service.payment.PaymentProviderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Payment processing controller
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payment Processing", description = "Payment processing and management endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class PaymentController {

    private final PaymentService paymentService;
    private final OrderRepository orderRepository;

    @Operation(summary = "Create payment intent", 
               description = "Create a payment intent for processing payment")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payment intent created successfully",
                     content = @Content(schema = @Schema(implementation = PaymentIntentResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "User not authenticated",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Order not found",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/intent")
    public ResponseEntity<PaymentIntentResponse> createPaymentIntent(@Valid @RequestBody PaymentIntentRequest request) {
        User currentUser = getCurrentUser();
        log.info("Creating payment intent for user: {} order: {}", currentUser.getEmail(), request.getOrderId());

        Order order = orderRepository.findById(request.getOrderId())
            .orElseThrow(() -> new RuntimeException("Order not found"));

        // Verify order belongs to current user
        if (!order.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Order does not belong to current user");
        }

        PaymentProviderService.PaymentIntent paymentIntent = paymentService.createPaymentIntent(order, request.getProvider());
        PaymentIntentResponse response = PaymentIntentResponse.fromPaymentIntent(paymentIntent);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Process payment", 
               description = "Process payment for an order")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payment processed successfully",
                     content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data or payment failed",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "User not authenticated",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Order not found",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/process")
    public ResponseEntity<PaymentResponse> processPayment(@Valid @RequestBody ProcessPaymentRequest request) {
        User currentUser = getCurrentUser();
        log.info("Processing payment for user: {} order: {}", currentUser.getEmail(), request.getOrderId());

        Order order = orderRepository.findById(request.getOrderId())
            .orElseThrow(() -> new RuntimeException("Order not found"));

        // Verify order belongs to current user
        if (!order.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Order does not belong to current user");
        }

        Payment payment = paymentService.processPayment(order, request.getPaymentMethodId(), request.getProvider());
        
        // Save payment method if requested
        if (request.getSavePaymentMethod() && request.getPaymentMethodId().startsWith("pm_")) {
            try {
                paymentService.savePaymentMethod(currentUser, request.getPaymentMethodId(), 
                    request.getProvider(), request.getSetAsDefault());
            } catch (Exception e) {
                log.warn("Failed to save payment method for user: {}", currentUser.getEmail(), e);
            }
        }

        PaymentResponse response = PaymentResponse.fromEntity(payment);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Capture payment", 
               description = "Capture an authorized payment")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payment captured successfully",
                     content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
        @ApiResponse(responseCode = "400", description = "Payment cannot be captured",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "User not authenticated",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Insufficient privileges",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Payment not found",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{paymentId}/capture")
    @PreAuthorize("hasRole('STORE_OWNER') or hasRole('ADMINISTRATOR')")
    public ResponseEntity<PaymentResponse> capturePayment(
            @Parameter(description = "Payment ID", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID paymentId,
            @RequestBody(required = false) CapturePaymentRequest request) {
        
        log.info("Capturing payment: {}", paymentId);

        BigDecimal amount = (request != null) ? request.getAmount() : null;
        Payment payment = paymentService.capturePayment(paymentId, amount);
        
        PaymentResponse response = PaymentResponse.fromEntity(payment);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Refund payment", 
               description = "Refund a payment partially or fully")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payment refunded successfully",
                     content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid refund request",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "User not authenticated",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Insufficient privileges",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Payment not found",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/refund")
    @PreAuthorize("hasRole('STORE_OWNER') or hasRole('ADMINISTRATOR')")
    public ResponseEntity<PaymentResponse> refundPayment(@Valid @RequestBody RefundPaymentRequest request) {
        log.info("Processing refund for payment: {} amount: {}", request.getPaymentId(), request.getAmount());

        Payment payment = paymentService.refundPayment(request.getPaymentId(), request.getAmount(), request.getReason());
        
        PaymentResponse response = PaymentResponse.fromEntity(payment);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Cancel payment", 
               description = "Cancel a pending payment")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payment cancelled successfully",
                     content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
        @ApiResponse(responseCode = "400", description = "Payment cannot be cancelled",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "User not authenticated",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Insufficient privileges",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Payment not found",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{paymentId}/cancel")
    @PreAuthorize("hasRole('STORE_OWNER') or hasRole('ADMINISTRATOR')")
    public ResponseEntity<PaymentResponse> cancelPayment(
            @Parameter(description = "Payment ID", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID paymentId) {
        
        log.info("Cancelling payment: {}", paymentId);

        Payment payment = paymentService.cancelPayment(paymentId);
        
        PaymentResponse response = PaymentResponse.fromEntity(payment);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get order payments", 
               description = "Get all payments for a specific order")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Order payments retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "User not authenticated",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Order not found",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<PaymentResponse>> getOrderPayments(
            @Parameter(description = "Order ID", example = "550e8400-e29b-41d4-a716-446655440001")
            @PathVariable UUID orderId) {
        
        User currentUser = getCurrentUser();
        
        // Verify order exists and belongs to user (unless admin/store owner)
        if (!hasAdminRole(currentUser)) {
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
            
            if (!order.getUser().getId().equals(currentUser.getId())) {
                throw new RuntimeException("Order does not belong to current user");
            }
        }

        List<Payment> payments = paymentService.getOrderPayments(orderId);
        List<PaymentResponse> response = payments.stream()
            .map(PaymentResponse::fromEntity)
            .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get user payments", 
               description = "Get paginated list of user's payments")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User payments retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "User not authenticated",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/my")
    public ResponseEntity<Page<PaymentResponse>> getUserPayments(
            @PageableDefault(size = 20) Pageable pageable) {
        
        User currentUser = getCurrentUser();
        log.debug("Getting payments for user: {}", currentUser.getEmail());

        Page<Payment> payments = paymentService.getUserPayments(currentUser.getId(), pageable);
        Page<PaymentResponse> response = payments.map(PaymentResponse::fromEntity);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Save payment method", 
               description = "Save a payment method for future use")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payment method saved successfully",
                     content = @Content(schema = @Schema(implementation = PaymentMethodResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid payment method or duplicate",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "User not authenticated",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/methods")
    public ResponseEntity<PaymentMethodResponse> savePaymentMethod(@Valid @RequestBody SavePaymentMethodRequest request) {
        User currentUser = getCurrentUser();
        log.info("Saving payment method for user: {}", currentUser.getEmail());

        PaymentMethod paymentMethod = paymentService.savePaymentMethod(
            currentUser, 
            request.getPaymentMethodToken(), 
            request.getProvider(),
            request.getSetAsDefault()
        );

        PaymentMethodResponse response = PaymentMethodResponse.fromEntity(paymentMethod);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get user payment methods", 
               description = "Get all active payment methods for current user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payment methods retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "User not authenticated",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/methods")
    public ResponseEntity<List<PaymentMethodResponse>> getUserPaymentMethods() {
        User currentUser = getCurrentUser();
        log.debug("Getting payment methods for user: {}", currentUser.getEmail());

        List<PaymentMethod> paymentMethods = paymentService.getUserPaymentMethods(currentUser);
        List<PaymentMethodResponse> response = paymentMethods.stream()
            .map(PaymentMethodResponse::fromEntity)
            .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Set default payment method", 
               description = "Set a payment method as default")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Default payment method set successfully"),
        @ApiResponse(responseCode = "401", description = "User not authenticated",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Payment method not found",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/methods/{paymentMethodId}/default")
    public ResponseEntity<Void> setDefaultPaymentMethod(
            @Parameter(description = "Payment Method ID", example = "550e8400-e29b-41d4-a716-446655440002")
            @PathVariable UUID paymentMethodId) {
        
        User currentUser = getCurrentUser();
        log.info("Setting default payment method for user: {} method: {}", currentUser.getEmail(), paymentMethodId);

        paymentService.setDefaultPaymentMethod(currentUser, paymentMethodId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Remove payment method", 
               description = "Remove a saved payment method")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payment method removed successfully"),
        @ApiResponse(responseCode = "401", description = "User not authenticated",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Payment method not found",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/methods/{paymentMethodId}")
    public ResponseEntity<Void> removePaymentMethod(
            @Parameter(description = "Payment Method ID", example = "550e8400-e29b-41d4-a716-446655440002")
            @PathVariable UUID paymentMethodId) {
        
        User currentUser = getCurrentUser();
        log.info("Removing payment method for user: {} method: {}", currentUser.getEmail(), paymentMethodId);

        paymentService.removePaymentMethod(currentUser, paymentMethodId);
        return ResponseEntity.ok().build();
    }

    /**
     * Get current authenticated user
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (User) authentication.getPrincipal();
    }

    /**
     * Check if user has admin role
     */
    private boolean hasAdminRole(User user) {
        return user.getRole().name().equals("ADMINISTRATOR") || user.getRole().name().equals("STORE_OWNER");
    }

    /**
     * Capture payment request DTO
     */
    @lombok.Data
    @Schema(description = "Capture payment request")
    public static class CapturePaymentRequest {
        @Schema(description = "Amount to capture (leave null for full amount)", example = "99.99")
        private BigDecimal amount;
    }
}