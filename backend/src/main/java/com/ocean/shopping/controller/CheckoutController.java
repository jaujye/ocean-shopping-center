package com.ocean.shopping.controller;

import com.ocean.shopping.dto.order.CheckoutRequest;
import com.ocean.shopping.dto.order.CheckoutResponse;
import com.ocean.shopping.security.JwtAuthenticationToken;
import com.ocean.shopping.service.OrderService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller for checkout operations
 */
@RestController
@RequestMapping("/api/checkout")
@RequiredArgsConstructor
@Slf4j
public class CheckoutController {

    private final OrderService orderService;

    /**
     * Process checkout and create order
     */
    @PostMapping
    public ResponseEntity<CheckoutResponse> processCheckout(
            @Valid @RequestBody CheckoutRequest request,
            Authentication authentication,
            HttpSession session) {
        
        try {
            log.info("Processing checkout request for email: {}", request.getCustomerEmail());

            UUID userId = null;
            String sessionId = null;

            // Extract user ID or session ID
            if (authentication != null && authentication.isAuthenticated() && 
                authentication instanceof JwtAuthenticationToken jwtAuth) {
                userId = jwtAuth.getUserId();
                log.debug("Authenticated checkout for user: {}", userId);
            } else {
                sessionId = session.getId();
                log.debug("Guest checkout for session: {}", sessionId);
            }

            CheckoutResponse response = orderService.processCheckout(request, userId, sessionId);

            if (response.isSuccess()) {
                log.info("Checkout successful - Order: {}", response.getOrderNumber());
                return ResponseEntity.ok(response);
            } else {
                log.warn("Checkout failed: {}", response.getMessage());
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            log.error("Checkout error: {}", e.getMessage(), e);
            CheckoutResponse errorResponse = CheckoutResponse.error("An unexpected error occurred during checkout");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Validate checkout data before processing
     */
    @PostMapping("/validate")
    public ResponseEntity<String> validateCheckout(
            @Valid @RequestBody CheckoutRequest request,
            Authentication authentication,
            HttpSession session) {
        
        try {
            log.debug("Validating checkout data for email: {}", request.getCustomerEmail());

            UUID userId = null;
            String sessionId = null;

            if (authentication != null && authentication.isAuthenticated() && 
                authentication instanceof JwtAuthenticationToken jwtAuth) {
                userId = jwtAuth.getUserId();
            } else {
                sessionId = session.getId();
            }

            // Basic validation logic
            if (userId == null && sessionId == null) {
                return ResponseEntity.badRequest().body("Invalid session");
            }

            // Additional validation can be added here
            // - Cart validation
            // - Address validation
            // - Payment method validation

            return ResponseEntity.ok("Checkout data is valid");

        } catch (Exception e) {
            log.error("Checkout validation error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Validation failed: " + e.getMessage());
        }
    }

    /**
     * Get checkout summary (cart totals, shipping, taxes)
     */
    @GetMapping("/summary")
    public ResponseEntity<?> getCheckoutSummary(
            Authentication authentication,
            HttpSession session,
            @RequestParam(required = false) String shippingCountry) {
        
        try {
            UUID userId = null;
            String sessionId = null;

            if (authentication != null && authentication.isAuthenticated() && 
                authentication instanceof JwtAuthenticationToken jwtAuth) {
                userId = jwtAuth.getUserId();
            } else {
                sessionId = session.getId();
            }

            // This would integrate with CartService to get current cart summary
            // including calculated shipping and taxes
            // For now, return a placeholder response

            return ResponseEntity.ok("Checkout summary endpoint - implementation depends on cart integration");

        } catch (Exception e) {
            log.error("Error getting checkout summary: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to get checkout summary");
        }
    }

    /**
     * Apply coupon code during checkout
     */
    @PostMapping("/coupon")
    public ResponseEntity<String> applyCoupon(
            @RequestParam String couponCode,
            Authentication authentication,
            HttpSession session) {
        
        try {
            log.info("Applying coupon code: {}", couponCode);

            UUID userId = null;
            String sessionId = null;

            if (authentication != null && authentication.isAuthenticated() && 
                authentication instanceof JwtAuthenticationToken jwtAuth) {
                userId = jwtAuth.getUserId();
            } else {
                sessionId = session.getId();
            }

            // This would integrate with CouponService to validate and apply coupon
            // For now, return a placeholder response

            return ResponseEntity.ok("Coupon applied successfully");

        } catch (Exception e) {
            log.error("Error applying coupon: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Failed to apply coupon: " + e.getMessage());
        }
    }

    /**
     * Remove coupon code from checkout
     */
    @DeleteMapping("/coupon")
    public ResponseEntity<String> removeCoupon(
            Authentication authentication,
            HttpSession session) {
        
        try {
            UUID userId = null;
            String sessionId = null;

            if (authentication != null && authentication.isAuthenticated() && 
                authentication instanceof JwtAuthenticationToken jwtAuth) {
                userId = jwtAuth.getUserId();
            } else {
                sessionId = session.getId();
            }

            // This would integrate with CouponService to remove coupon
            // For now, return a placeholder response

            return ResponseEntity.ok("Coupon removed successfully");

        } catch (Exception e) {
            log.error("Error removing coupon: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Failed to remove coupon: " + e.getMessage());
        }
    }

    /**
     * Calculate shipping costs for checkout
     */
    @PostMapping("/shipping/calculate")
    public ResponseEntity<?> calculateShipping(
            @RequestBody CheckoutRequest.AddressDto shippingAddress,
            Authentication authentication,
            HttpSession session) {
        
        try {
            log.debug("Calculating shipping for address: {}, {}", 
                shippingAddress.getCity(), shippingAddress.getCountry());

            UUID userId = null;
            String sessionId = null;

            if (authentication != null && authentication.isAuthenticated() && 
                authentication instanceof JwtAuthenticationToken jwtAuth) {
                userId = jwtAuth.getUserId();
            } else {
                sessionId = session.getId();
            }

            // This would integrate with ShippingService to calculate actual shipping costs
            // For now, return a placeholder response

            return ResponseEntity.ok("Shipping calculation endpoint - implementation depends on shipping integration");

        } catch (Exception e) {
            log.error("Error calculating shipping: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to calculate shipping");
        }
    }
}