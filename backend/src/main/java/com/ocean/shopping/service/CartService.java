package com.ocean.shopping.service;

import com.ocean.shopping.model.entity.*;
import com.ocean.shopping.repository.CartRepository;
import com.ocean.shopping.repository.CartItemRepository;
import com.ocean.shopping.repository.ProductRepository;
import com.ocean.shopping.exception.BadRequestException;
import com.ocean.shopping.exception.ResourceNotFoundException;
import com.ocean.shopping.service.lock.DistributedLockManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Cart service for managing shopping cart operations with Redis session support.
 * Handles both authenticated user carts and guest session-based carts.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserService userService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final DistributedLockManager lockManager;

    // Constants
    private static final String CART_CACHE_PREFIX = "cart:";
    private static final String SESSION_CART_PREFIX = "session_cart:";
    private static final int CART_EXPIRATION_DAYS = 30;
    private static final int ABANDONED_CART_THRESHOLD_HOURS = 24;

    /**
     * Get or create cart for authenticated user
     */
    @Transactional(readOnly = true)
    public Cart getUserCart(UUID userId) {
        log.debug("Getting cart for user: {}", userId);

        User user = userService.getUserById(userId);
        
        return cartRepository.findByUserIdAndStatus(userId, Cart.CartStatus.ACTIVE)
                .orElseGet(() -> createUserCart(user));
    }

    /**
     * Get or create cart for session (guest user)
     */
    @Transactional(readOnly = true)
    public Cart getSessionCart(String sessionId) {
        log.debug("Getting cart for session: {}", sessionId);

        if (!StringUtils.hasText(sessionId)) {
            throw new BadRequestException("Session ID is required");
        }

        // Try cache first
        String cacheKey = SESSION_CART_PREFIX + sessionId;
        Cart cachedCart = (Cart) redisTemplate.opsForValue().get(cacheKey);
        if (cachedCart != null) {
            log.debug("Found cached cart for session: {}", sessionId);
            return cachedCart;
        }

        // Try database
        Optional<Cart> dbCart = cartRepository.findBySessionIdAndStatus(sessionId, Cart.CartStatus.ACTIVE);
        if (dbCart.isPresent()) {
            // Cache the cart
            cacheCart(dbCart.get());
            return dbCart.get();
        }

        // Create new session cart
        return createSessionCart(sessionId);
    }

    /**
     * Add item to cart with distributed lock protection
     */
    @Transactional
    public Cart addItemToCart(UUID userId, String sessionId, UUID productId, Integer quantity, 
                             UUID productVariantId, Map<String, String> selectedOptions) {
        log.debug("Adding item to cart - User: {}, Session: {}, Product: {}, Quantity: {}", 
                  userId, sessionId, productId, quantity);

        if (quantity == null || quantity < 1) {
            throw new BadRequestException("Quantity must be at least 1");
        }

        // Use distributed locks to prevent race conditions
        String userIdentifier = userId != null ? userId.toString() : sessionId;
        String cartLockKey = lockManager.cartLockKey(userIdentifier);
        String inventoryLockKey = lockManager.inventoryLockKey(productId.toString());
        
        // Execute cart operation with distributed lock protection
        return lockManager.executeWithLockOrThrow(cartLockKey, () -> {
            return lockManager.executeWithLockOrThrow(inventoryLockKey, () -> {
                return addItemToCartInternal(userId, sessionId, productId, quantity, productVariantId, selectedOptions);
            });
        });
    }

    /**
     * Internal method for adding item to cart (protected by locks)
     */
    private Cart addItemToCartInternal(UUID userId, String sessionId, UUID productId, Integer quantity, 
                                      UUID productVariantId, Map<String, String> selectedOptions) {
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ResourceNotFoundException.forEntity("Product", productId));

        if (!product.getIsActive()) {
            throw new BadRequestException("Product is not active");
        }

        // Check inventory with fresh data (within lock)
        if (product.getTrackInventory() && quantity > product.getInventoryQuantity()) {
            throw new BadRequestException("Insufficient inventory. Available: " + product.getInventoryQuantity());
        }

        Cart cart = userId != null ? getUserCart(userId) : getSessionCart(sessionId);
        
        // Check if item already exists in cart
        Optional<CartItem> existingItem = findExistingCartItem(cart, productId, productVariantId, selectedOptions);
        
        if (existingItem.isPresent()) {
            // Update quantity of existing item
            CartItem item = existingItem.get();
            int newQuantity = item.getQuantity() + quantity;
            
            // Re-check inventory with new total quantity
            if (product.getTrackInventory() && newQuantity > product.getInventoryQuantity()) {
                throw new BadRequestException("Total quantity exceeds available inventory");
            }
            
            item.updateQuantity(newQuantity);
            cartItemRepository.save(item);
        } else {
            // Create new cart item
            CartItem cartItem = createCartItem(cart, product, productVariantId, quantity, selectedOptions);
            cart.addItem(cartItem);
            cartItemRepository.save(cartItem);
        }

        cart.recalculateTotals();
        Cart savedCart = cartRepository.save(cart);
        
        // Update cache
        cacheCart(savedCart);
        
        log.debug("Item added to cart successfully. Cart total: {}", savedCart.getTotal());
        return savedCart;
    }

    /**
     * Update cart item quantity with distributed lock protection
     */
    @Transactional
    public Cart updateCartItemQuantity(UUID userId, String sessionId, UUID itemId, Integer quantity) {
        log.debug("Updating cart item quantity - User: {}, Session: {}, Item: {}, Quantity: {}", 
                  userId, sessionId, itemId, quantity);

        if (quantity == null || quantity < 1) {
            throw new BadRequestException("Quantity must be at least 1");
        }

        // First get the cart item to determine locks needed
        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> ResourceNotFoundException.forEntity("CartItem", itemId));

        String userIdentifier = userId != null ? userId.toString() : sessionId;
        String cartLockKey = lockManager.cartLockKey(userIdentifier);
        String inventoryLockKey = lockManager.inventoryLockKey(cartItem.getProduct().getId().toString());
        
        // Execute update with distributed lock protection
        return lockManager.executeWithLockOrThrow(cartLockKey, () -> {
            return lockManager.executeWithLockOrThrow(inventoryLockKey, () -> {
                return updateCartItemQuantityInternal(userId, sessionId, itemId, quantity);
            });
        });
    }

    /**
     * Internal method for updating cart item quantity (protected by locks)
     */
    private Cart updateCartItemQuantityInternal(UUID userId, String sessionId, UUID itemId, Integer quantity) {
        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> ResourceNotFoundException.forEntity("CartItem", itemId));

        Cart cart = cartItem.getCart();
        
        // Verify ownership
        if (!verifyCartOwnership(cart, userId, sessionId)) {
            throw new BadRequestException("Cart item does not belong to user or session");
        }

        // Check inventory with fresh data (within lock)
        Product product = cartItem.getProduct();
        if (product.getTrackInventory() && quantity > product.getInventoryQuantity()) {
            throw new BadRequestException("Insufficient inventory. Available: " + product.getInventoryQuantity());
        }

        cartItem.updateQuantity(quantity);
        cartItemRepository.save(cartItem);

        cart.recalculateTotals();
        Cart savedCart = cartRepository.save(cart);
        
        // Update cache
        cacheCart(savedCart);
        
        log.debug("Cart item quantity updated successfully");
        return savedCart;
    }

    /**
     * Remove item from cart with distributed lock protection
     */
    @Transactional
    public Cart removeCartItem(UUID userId, String sessionId, UUID itemId) {
        log.debug("Removing cart item - User: {}, Session: {}, Item: {}", userId, sessionId, itemId);

        String userIdentifier = userId != null ? userId.toString() : sessionId;
        String cartLockKey = lockManager.cartLockKey(userIdentifier);
        
        // Execute removal with distributed lock protection
        return lockManager.executeWithLockOrThrow(cartLockKey, () -> {
            return removeCartItemInternal(userId, sessionId, itemId);
        });
    }

    /**
     * Internal method for removing cart item (protected by locks)
     */
    private Cart removeCartItemInternal(UUID userId, String sessionId, UUID itemId) {
        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> ResourceNotFoundException.forEntity("CartItem", itemId));

        Cart cart = cartItem.getCart();
        
        // Verify ownership
        if (!verifyCartOwnership(cart, userId, sessionId)) {
            throw new BadRequestException("Cart item does not belong to user or session");
        }

        cart.removeItem(cartItem);
        cartItemRepository.delete(cartItem);

        cart.recalculateTotals();
        Cart savedCart = cartRepository.save(cart);
        
        // Update cache
        cacheCart(savedCart);
        
        log.debug("Cart item removed successfully");
        return savedCart;
    }

    /**
     * Clear entire cart
     */
    @Transactional
    public void clearCart(UUID userId, String sessionId) {
        log.debug("Clearing cart - User: {}, Session: {}", userId, sessionId);

        Cart cart = userId != null ? 
                cartRepository.findByUserIdAndStatus(userId, Cart.CartStatus.ACTIVE).orElse(null) :
                cartRepository.findBySessionIdAndStatus(sessionId, Cart.CartStatus.ACTIVE).orElse(null);

        if (cart != null) {
            cart.clearItems();
            cartItemRepository.deleteAllByCartId(cart.getId());
            
            cart.recalculateTotals();
            cartRepository.save(cart);
            
            // Clear cache
            clearCartCache(cart);
            
            log.debug("Cart cleared successfully");
        }
    }

    /**
     * Move item to wishlist (save for later)
     */
    @Transactional
    public Cart moveItemToWishlist(UUID userId, String sessionId, UUID itemId) {
        log.debug("Moving item to wishlist - User: {}, Session: {}, Item: {}", userId, sessionId, itemId);

        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> ResourceNotFoundException.forEntity("CartItem", itemId));

        Cart cart = cartItem.getCart();
        
        // Verify ownership
        if (!verifyCartOwnership(cart, userId, sessionId)) {
            throw new BadRequestException("Cart item does not belong to user or session");
        }

        cartItem.saveForLater();
        cartItemRepository.save(cartItem);

        cart.recalculateTotals();
        Cart savedCart = cartRepository.save(cart);
        
        // Update cache
        cacheCart(savedCart);
        
        log.debug("Item moved to wishlist successfully");
        return savedCart;
    }

    /**
     * Move item from wishlist back to cart
     */
    @Transactional
    public Cart moveItemToCart(UUID userId, String sessionId, UUID itemId) {
        log.debug("Moving item to cart from wishlist - User: {}, Session: {}, Item: {}", userId, sessionId, itemId);

        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> ResourceNotFoundException.forEntity("CartItem", itemId));

        Cart cart = cartItem.getCart();
        
        // Verify ownership
        if (!verifyCartOwnership(cart, userId, sessionId)) {
            throw new BadRequestException("Cart item does not belong to user or session");
        }

        // Check inventory before moving back
        Product product = cartItem.getProduct();
        if (product.getTrackInventory() && cartItem.getQuantity() > product.getInventoryQuantity()) {
            throw new BadRequestException("Insufficient inventory to move item back to cart");
        }

        cartItem.moveToCart();
        cartItemRepository.save(cartItem);

        cart.recalculateTotals();
        Cart savedCart = cartRepository.save(cart);
        
        // Update cache
        cacheCart(savedCart);
        
        log.debug("Item moved to cart from wishlist successfully");
        return savedCart;
    }

    /**
     * Apply coupon to cart
     */
    @Transactional
    public Cart applyCoupon(UUID userId, String sessionId, String couponCode) {
        log.debug("Applying coupon to cart - User: {}, Session: {}, Coupon: {}", userId, sessionId, couponCode);

        Cart cart = userId != null ? getUserCart(userId) : getSessionCart(sessionId);
        
        // TODO: Implement coupon validation and discount calculation
        // This will be implemented in Stream 4
        
        // For now, just store the coupon code
        cart.applyCoupon(couponCode, BigDecimal.ZERO);
        Cart savedCart = cartRepository.save(cart);
        
        // Update cache
        cacheCart(savedCart);
        
        log.debug("Coupon applied to cart successfully");
        return savedCart;
    }

    /**
     * Remove coupon from cart
     */
    @Transactional
    public Cart removeCoupon(UUID userId, String sessionId) {
        log.debug("Removing coupon from cart - User: {}, Session: {}", userId, sessionId);

        Cart cart = userId != null ? getUserCart(userId) : getSessionCart(sessionId);
        
        cart.removeCoupon();
        Cart savedCart = cartRepository.save(cart);
        
        // Update cache
        cacheCart(savedCart);
        
        log.debug("Coupon removed from cart successfully");
        return savedCart;
    }

    /**
     * Merge guest cart with user cart when user logs in
     */
    @Transactional
    public Cart mergeGuestCart(UUID userId, String guestSessionId) {
        log.debug("Merging guest cart with user cart - User: {}, Guest Session: {}", userId, guestSessionId);

        Cart userCart = getUserCart(userId);
        Optional<Cart> guestCartOpt = cartRepository.findBySessionIdAndStatus(guestSessionId, Cart.CartStatus.ACTIVE);

        if (guestCartOpt.isEmpty()) {
            log.debug("No guest cart found for session: {}", guestSessionId);
            return userCart;
        }

        Cart guestCart = guestCartOpt.get();
        List<CartItem> guestItems = cartItemRepository.findByCartIdOrderByCreatedAt(guestCart.getId());

        for (CartItem guestItem : guestItems) {
            // Check if similar item exists in user cart
            Optional<CartItem> existingItem = findExistingCartItem(userCart, 
                    guestItem.getProduct().getId(), 
                    guestItem.getProductVariant() != null ? guestItem.getProductVariant().getId() : null,
                    guestItem.getSelectedOptions());

            if (existingItem.isPresent()) {
                // Merge quantities
                CartItem userItem = existingItem.get();
                int newQuantity = userItem.getQuantity() + guestItem.getQuantity();
                
                // Check inventory limits
                Product product = userItem.getProduct();
                if (product.getTrackInventory() && newQuantity > product.getInventoryQuantity()) {
                    newQuantity = product.getInventoryQuantity();
                    log.warn("Limited merged quantity due to inventory constraints for product: {}", product.getId());
                }
                
                userItem.updateQuantity(newQuantity);
                cartItemRepository.save(userItem);
            } else {
                // Move item to user cart
                guestItem.setCart(userCart);
                userCart.addItem(guestItem);
                cartItemRepository.save(guestItem);
            }
        }

        // Mark guest cart as merged
        guestCart.markAsMerged(guestSessionId);
        cartRepository.save(guestCart);

        userCart.recalculateTotals();
        Cart savedUserCart = cartRepository.save(userCart);
        
        // Update cache
        cacheCart(savedUserCart);
        clearCartCache(guestCart);
        
        log.debug("Guest cart merged successfully. Total items: {}", savedUserCart.getTotalItems());
        return savedUserCart;
    }

    /**
     * Get cart item count for user or session
     */
    @Transactional(readOnly = true)
    public int getCartItemCount(UUID userId, String sessionId) {
        if (userId != null) {
            return (int) cartItemRepository.countActiveItemsByUserId(userId);
        } else if (StringUtils.hasText(sessionId)) {
            Cart cart = getSessionCart(sessionId);
            return cart.getTotalItems();
        }
        return 0;
    }

    /**
     * Validate cart items (check inventory, pricing, etc.)
     */
    @Transactional
    public List<String> validateCart(UUID userId, String sessionId) {
        log.debug("Validating cart - User: {}, Session: {}", userId, sessionId);

        Cart cart = userId != null ? getUserCart(userId) : getSessionCart(sessionId);
        List<String> issues = new ArrayList<>();
        boolean cartUpdated = false;

        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();

            // Check if product is still active
            if (!product.getIsActive()) {
                issues.add("Product '" + product.getName() + "' is no longer available");
                continue;
            }

            // Check inventory
            if (product.getTrackInventory() && item.getQuantity() > product.getInventoryQuantity()) {
                if (product.getInventoryQuantity() > 0) {
                    item.updateQuantity(product.getInventoryQuantity());
                    cartItemRepository.save(item);
                    cartUpdated = true;
                    issues.add("Quantity reduced for '" + product.getName() + "' due to limited stock");
                } else {
                    issues.add("Product '" + product.getName() + "' is out of stock");
                }
            }

            // Check price changes
            if (!item.getUnitPrice().equals(product.getPrice())) {
                item.setOriginalUnitPrice(item.getUnitPrice());
                item.setUnitPrice(product.getPrice());
                cartItemRepository.save(item);
                cartUpdated = true;
                issues.add("Price updated for '" + product.getName() + "'");
            }
        }

        if (cartUpdated) {
            cart.recalculateTotals();
            cartRepository.save(cart);
            cacheCart(cart);
        }

        log.debug("Cart validation completed. Issues found: {}", issues.size());
        return issues;
    }

    /**
     * Mark cart as converted (when order is placed)
     */
    @Transactional
    public void markCartAsConverted(UUID cartId) {
        log.debug("Marking cart as converted: {}", cartId);

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> ResourceNotFoundException.forEntity("Cart", cartId));

        cart.markAsConverted();
        cartRepository.save(cart);
        
        // Clear cache
        clearCartCache(cart);
        
        log.debug("Cart marked as converted successfully");
    }

    /**
     * Clean up abandoned carts
     */
    @Transactional
    public int cleanupAbandonedCarts() {
        log.debug("Cleaning up abandoned carts");

        ZonedDateTime threshold = ZonedDateTime.now().minusHours(ABANDONED_CART_THRESHOLD_HOURS);
        int markedAsAbandoned = cartRepository.markOldCartsAsAbandoned(threshold);
        
        // Clean up expired carts
        int markedAsExpired = cartRepository.markExpiredCarts(ZonedDateTime.now());
        
        // Delete old processed carts
        ZonedDateTime deleteThreshold = ZonedDateTime.now().minusDays(90);
        int deleted = cartRepository.deleteOldProcessedCarts(deleteThreshold);
        
        log.debug("Abandoned cart cleanup completed. Marked abandoned: {}, Expired: {}, Deleted: {}", 
                  markedAsAbandoned, markedAsExpired, deleted);
        
        return markedAsAbandoned + markedAsExpired + deleted;
    }

    // Private helper methods

    private Cart createUserCart(User user) {
        log.debug("Creating new cart for user: {}", user.getId());

        Cart cart = Cart.builder()
                .user(user)
                .status(Cart.CartStatus.ACTIVE)
                .expiresAt(ZonedDateTime.now().plusDays(CART_EXPIRATION_DAYS))
                .build();

        return cartRepository.save(cart);
    }

    private Cart createSessionCart(String sessionId) {
        log.debug("Creating new session cart: {}", sessionId);

        Cart cart = Cart.builder()
                .sessionId(sessionId)
                .status(Cart.CartStatus.ACTIVE)
                .expiresAt(ZonedDateTime.now().plusDays(CART_EXPIRATION_DAYS))
                .build();

        Cart savedCart = cartRepository.save(cart);
        cacheCart(savedCart);
        return savedCart;
    }

    private CartItem createCartItem(Cart cart, Product product, UUID productVariantId, 
                                  Integer quantity, Map<String, String> selectedOptions) {
        ProductVariant variant = null;
        if (productVariantId != null) {
            // TODO: Fetch product variant when variant system is implemented
        }

        return CartItem.builder()
                .cart(cart)
                .product(product)
                .productVariant(variant)
                .quantity(quantity)
                .unitPrice(product.getPrice())
                .selectedOptions(selectedOptions)
                .build();
    }

    private Optional<CartItem> findExistingCartItem(Cart cart, UUID productId, UUID productVariantId, 
                                                  Map<String, String> selectedOptions) {
        return cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .filter(item -> {
                    if (productVariantId == null) {
                        return item.getProductVariant() == null;
                    } else {
                        return item.getProductVariant() != null && 
                               item.getProductVariant().getId().equals(productVariantId);
                    }
                })
                .filter(item -> {
                    if (selectedOptions == null || selectedOptions.isEmpty()) {
                        return item.getSelectedOptions() == null || item.getSelectedOptions().isEmpty();
                    } else {
                        return Objects.equals(item.getSelectedOptions(), selectedOptions);
                    }
                })
                .findFirst();
    }

    private boolean verifyCartOwnership(Cart cart, UUID userId, String sessionId) {
        if (userId != null) {
            return cart.getUser() != null && cart.getUser().getId().equals(userId);
        } else if (StringUtils.hasText(sessionId)) {
            return cart.getSessionId() != null && cart.getSessionId().equals(sessionId);
        }
        return false;
    }

    private void cacheCart(Cart cart) {
        try {
            if (cart.getSessionId() != null) {
                String cacheKey = SESSION_CART_PREFIX + cart.getSessionId();
                redisTemplate.opsForValue().set(cacheKey, cart, CART_EXPIRATION_DAYS, TimeUnit.DAYS);
                log.debug("Cart cached for session: {}", cart.getSessionId());
            } else if (cart.getUser() != null) {
                String cacheKey = CART_CACHE_PREFIX + cart.getUser().getId();
                redisTemplate.opsForValue().set(cacheKey, cart, CART_EXPIRATION_DAYS, TimeUnit.DAYS);
                log.debug("Cart cached for user: {}", cart.getUser().getId());
            }
        } catch (Exception e) {
            log.warn("Failed to cache cart: {}", e.getMessage());
        }
    }

    private void clearCartCache(Cart cart) {
        try {
            if (cart.getSessionId() != null) {
                String cacheKey = SESSION_CART_PREFIX + cart.getSessionId();
                redisTemplate.delete(cacheKey);
            } else if (cart.getUser() != null) {
                String cacheKey = CART_CACHE_PREFIX + cart.getUser().getId();
                redisTemplate.delete(cacheKey);
            }
        } catch (Exception e) {
            log.warn("Failed to clear cart cache: {}", e.getMessage());
        }
    }
}