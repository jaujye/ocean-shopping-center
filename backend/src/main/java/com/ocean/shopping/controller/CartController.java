package com.ocean.shopping.controller;

import com.ocean.shopping.dto.cart.*;
import com.ocean.shopping.exception.ErrorResponse;
import com.ocean.shopping.model.entity.Cart;
import com.ocean.shopping.model.entity.User;
import com.ocean.shopping.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Cart controller handling shopping cart operations with session support.
 * Supports both authenticated users and guest sessions.
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Cart", description = "Shopping cart management endpoints")
public class CartController {

    private final CartService cartService;

    @Operation(summary = "Get current cart",
               description = "Get the current user's cart or session cart for guest users")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cart retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Authentication required for user cart",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<CartResponse> getCart(HttpServletRequest request) {
        log.debug("Getting cart for request");

        UUID userId = getCurrentUserId();
        String sessionId = getSessionId(request);

        Cart cart = userId != null ? 
                cartService.getUserCart(userId) : 
                cartService.getSessionCart(sessionId);

        return ResponseEntity.ok(CartResponse.fromEntity(cart));
    }

    @Operation(summary = "Get cart summary",
               description = "Get cart summary with minimal data (item count, totals)")
    @ApiResponse(responseCode = "200", description = "Cart summary retrieved successfully")
    @GetMapping("/summary")
    public ResponseEntity<CartResponse> getCartSummary(HttpServletRequest request) {
        log.debug("Getting cart summary for request");

        UUID userId = getCurrentUserId();
        String sessionId = getSessionId(request);

        Cart cart = userId != null ? 
                cartService.getUserCart(userId) : 
                cartService.getSessionCart(sessionId);

        return ResponseEntity.ok(CartResponse.summaryFromEntity(cart));
    }

    @Operation(summary = "Add item to cart",
               description = "Add a product to the cart with specified quantity and options")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Item added to cart successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request or insufficient inventory",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Product not found",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/add")
    public ResponseEntity<CartResponse> addToCart(
            @Valid @RequestBody AddToCartRequest request,
            HttpServletRequest httpRequest) {
        log.debug("Adding item to cart: {}", request);

        UUID userId = getCurrentUserId();
        String sessionId = getSessionId(httpRequest);

        Cart cart = cartService.addItemToCart(
                userId,
                sessionId,
                request.getProductId(),
                request.getQuantity(),
                request.getProductVariantId(),
                request.getSelectedOptions()
        );

        return ResponseEntity.ok(CartResponse.fromEntity(cart));
    }

    @Operation(summary = "Update cart item",
               description = "Update quantity or other properties of a cart item")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cart item updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request or insufficient inventory",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Cart item not found",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/items/{itemId}")
    public ResponseEntity<CartResponse> updateCartItem(
            @Parameter(description = "Cart item ID")
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateCartItemRequest request,
            HttpServletRequest httpRequest) {
        log.debug("Updating cart item: {} with request: {}", itemId, request);

        UUID userId = getCurrentUserId();
        String sessionId = getSessionId(httpRequest);

        Cart cart = cartService.updateCartItemQuantity(
                userId,
                sessionId,
                itemId,
                request.getQuantity()
        );

        return ResponseEntity.ok(CartResponse.fromEntity(cart));
    }

    @Operation(summary = "Remove item from cart",
               description = "Remove a specific item from the cart")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Item removed successfully"),
        @ApiResponse(responseCode = "404", description = "Cart item not found",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartResponse> removeCartItem(
            @Parameter(description = "Cart item ID")
            @PathVariable UUID itemId,
            HttpServletRequest httpRequest) {
        log.debug("Removing cart item: {}", itemId);

        UUID userId = getCurrentUserId();
        String sessionId = getSessionId(httpRequest);

        Cart cart = cartService.removeCartItem(userId, sessionId, itemId);

        return ResponseEntity.ok(CartResponse.fromEntity(cart));
    }

    @Operation(summary = "Clear entire cart",
               description = "Remove all items from the cart")
    @ApiResponse(responseCode = "204", description = "Cart cleared successfully")
    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearCart(HttpServletRequest request) {
        log.debug("Clearing cart");

        UUID userId = getCurrentUserId();
        String sessionId = getSessionId(request);

        cartService.clearCart(userId, sessionId);

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Move item to wishlist",
               description = "Move cart item to wishlist (save for later)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Item moved to wishlist successfully"),
        @ApiResponse(responseCode = "404", description = "Cart item not found",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/items/{itemId}/move-to-wishlist")
    public ResponseEntity<CartResponse> moveToWishlist(
            @Parameter(description = "Cart item ID")
            @PathVariable UUID itemId,
            HttpServletRequest request) {
        log.debug("Moving cart item to wishlist: {}", itemId);

        UUID userId = getCurrentUserId();
        String sessionId = getSessionId(request);

        Cart cart = cartService.moveItemToWishlist(userId, sessionId, itemId);

        return ResponseEntity.ok(CartResponse.fromEntity(cart));
    }

    @Operation(summary = "Move item to cart",
               description = "Move item from wishlist back to active cart")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Item moved to cart successfully"),
        @ApiResponse(responseCode = "404", description = "Cart item not found",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "400", description = "Insufficient inventory",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/items/{itemId}/move-to-cart")
    public ResponseEntity<CartResponse> moveToCart(
            @Parameter(description = "Cart item ID")
            @PathVariable UUID itemId,
            HttpServletRequest request) {
        log.debug("Moving item to cart from wishlist: {}", itemId);

        UUID userId = getCurrentUserId();
        String sessionId = getSessionId(request);

        Cart cart = cartService.moveItemToCart(userId, sessionId, itemId);

        return ResponseEntity.ok(CartResponse.fromEntity(cart));
    }

    @Operation(summary = "Apply coupon code",
               description = "Apply a coupon code to the cart for discount")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Coupon applied successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid or expired coupon",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/coupon")
    public ResponseEntity<CartResponse> applyCoupon(
            @Valid @RequestBody ApplyCouponRequest request,
            HttpServletRequest httpRequest) {
        log.debug("Applying coupon to cart: {}", request.getCouponCode());

        UUID userId = getCurrentUserId();
        String sessionId = getSessionId(httpRequest);

        Cart cart = cartService.applyCoupon(userId, sessionId, request.getCouponCode());

        return ResponseEntity.ok(CartResponse.fromEntity(cart));
    }

    @Operation(summary = "Remove coupon",
               description = "Remove applied coupon from the cart")
    @ApiResponse(responseCode = "200", description = "Coupon removed successfully")
    @DeleteMapping("/coupon")
    public ResponseEntity<CartResponse> removeCoupon(HttpServletRequest request) {
        log.debug("Removing coupon from cart");

        UUID userId = getCurrentUserId();
        String sessionId = getSessionId(request);

        Cart cart = cartService.removeCoupon(userId, sessionId);

        return ResponseEntity.ok(CartResponse.fromEntity(cart));
    }

    @Operation(summary = "Get cart item count",
               description = "Get the total number of items in the cart")
    @ApiResponse(responseCode = "200", description = "Cart item count retrieved successfully")
    @GetMapping("/count")
    public ResponseEntity<Map<String, Integer>> getCartItemCount(HttpServletRequest request) {
        log.debug("Getting cart item count");

        UUID userId = getCurrentUserId();
        String sessionId = getSessionId(request);

        int itemCount = cartService.getCartItemCount(userId, sessionId);

        return ResponseEntity.ok(Map.of("count", itemCount));
    }

    @Operation(summary = "Validate cart",
               description = "Validate cart items for inventory, pricing, and availability")
    @ApiResponse(responseCode = "200", description = "Cart validation completed")
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateCart(HttpServletRequest request) {
        log.debug("Validating cart");

        UUID userId = getCurrentUserId();
        String sessionId = getSessionId(request);

        List<String> issues = cartService.validateCart(userId, sessionId);

        return ResponseEntity.ok(Map.of(
                "valid", issues.isEmpty(),
                "issues", issues
        ));
    }

    @Operation(summary = "Merge guest cart",
               description = "Merge guest session cart with user cart on login",
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Carts merged successfully"),
        @ApiResponse(responseCode = "401", description = "Authentication required",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/merge")
    public ResponseEntity<CartResponse> mergeGuestCart(
            @Parameter(description = "Guest session ID")
            @RequestParam String guestSessionId) {
        log.debug("Merging guest cart with user cart - Guest session: {}", guestSessionId);

        UUID userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Cart cart = cartService.mergeGuestCart(userId, guestSessionId);

        return ResponseEntity.ok(CartResponse.fromEntity(cart));
    }

    // Private helper methods

    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && 
            authentication.getPrincipal() instanceof User) {
            User user = (User) authentication.getPrincipal();
            return user.getId();
        }
        return null;
    }

    private String getSessionId(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        return session.getId();
    }
}