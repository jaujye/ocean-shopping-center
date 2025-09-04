package com.ocean.shopping.service;

import com.ocean.shopping.dto.coupon.CouponResponse;
import com.ocean.shopping.dto.coupon.CouponValidationRequest;
import com.ocean.shopping.dto.coupon.CouponValidationResponse;
import com.ocean.shopping.exception.BadRequestException;
import com.ocean.shopping.exception.ResourceNotFoundException;
import com.ocean.shopping.model.entity.*;
import com.ocean.shopping.model.entity.enums.CouponStatus;
import com.ocean.shopping.model.entity.enums.CouponType;
import com.ocean.shopping.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * Service for coupon operations and discount calculations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CouponService {

    private final CouponRepository couponRepository;
    private final OrderCouponRepository orderCouponRepository;
    private final StoreRepository storeRepository;
    private final OrderRepository orderRepository;

    /**
     * Validate a coupon code and calculate discount
     */
    @Transactional(readOnly = true)
    public CouponValidationResponse validateCoupon(CouponValidationRequest request) {
        log.info("Validating coupon code: {}", request.getCode());

        try {
            // Find the coupon
            Optional<Coupon> couponOpt = request.getStoreId() != null 
                ? findCouponForStore(request.getCode(), request.getStoreId())
                : couponRepository.findUsableCouponByCode(request.getCode(), ZonedDateTime.now());

            if (couponOpt.isEmpty()) {
                return CouponValidationResponse.invalid(request.getCode(), "Coupon code not found or not valid");
            }

            Coupon coupon = couponOpt.get();

            // Validate coupon can be used
            String validationError = validateCouponUsage(coupon, request);
            if (validationError != null) {
                return CouponValidationResponse.invalid(request.getCode(), validationError);
            }

            // Calculate discount
            BigDecimal discountAmount = calculateDiscount(coupon, request.getOrderAmount());
            boolean freeShipping = coupon.getType() == CouponType.FREE_SHIPPING;

            return CouponValidationResponse.valid(
                coupon.getCode(),
                coupon.getName(),
                coupon.getType(),
                discountAmount,
                request.getOrderAmount(),
                request.getCurrency(),
                freeShipping
            );

        } catch (Exception e) {
            log.error("Error validating coupon {}: {}", request.getCode(), e.getMessage(), e);
            return CouponValidationResponse.invalid(request.getCode(), "An error occurred while validating the coupon");
        }
    }

    /**
     * Apply coupon to an order
     */
    @Transactional
    public OrderCoupon applyCouponToOrder(Order order, String couponCode) {
        log.info("Applying coupon {} to order {}", couponCode, order.getOrderNumber());

        // Validate the coupon
        CouponValidationRequest validationRequest = new CouponValidationRequest();
        validationRequest.setCode(couponCode);
        validationRequest.setOrderAmount(order.getSubtotal()); // Use subtotal before discount
        validationRequest.setStoreId(order.getStore().getId());
        validationRequest.setCustomerEmail(order.getCustomerEmail());
        validationRequest.setCurrency(order.getCurrency());

        CouponValidationResponse validation = validateCoupon(validationRequest);
        if (!validation.isValid()) {
            throw new BadRequestException(validation.getErrorMessage());
        }

        // Find the coupon entity
        Coupon coupon = couponRepository.findUsableCouponByCodeForStore(
            couponCode, 
            order.getStore(), 
            ZonedDateTime.now()
        ).orElseThrow(() -> new ResourceNotFoundException("Coupon not found: " + couponCode));

        // Check if coupon is already applied to this order
        if (orderCouponRepository.existsByOrderAndCoupon(order, coupon)) {
            throw new BadRequestException("Coupon is already applied to this order");
        }

        // Create OrderCoupon record
        OrderCoupon orderCoupon = OrderCoupon.builder()
            .order(order)
            .coupon(coupon)
            .couponCode(coupon.getCode())
            .couponName(coupon.getName())
            .discountAmount(validation.getDiscountAmount())
            .originalOrderAmount(order.getSubtotal())
            .currency(order.getCurrency())
            .build();

        orderCoupon = orderCouponRepository.save(orderCoupon);

        // Update coupon usage
        coupon.incrementUsage();
        couponRepository.save(coupon);

        // Update order discount amount
        BigDecimal newDiscountAmount = order.getDiscountAmount().add(validation.getDiscountAmount());
        order.setDiscountAmount(newDiscountAmount);
        
        // Recalculate total (subtotal + tax + shipping - discount)
        BigDecimal newTotal = order.getSubtotal()
            .add(order.getTaxAmount())
            .add(order.getShippingAmount())
            .subtract(newDiscountAmount);
        order.setTotalAmount(newTotal);

        orderRepository.save(order);

        log.info("Successfully applied coupon {} to order {} with discount {}", 
                couponCode, order.getOrderNumber(), validation.getDiscountAmount());

        return orderCoupon;
    }

    /**
     * Remove coupon from order
     */
    @Transactional
    public void removeCouponFromOrder(Order order, String couponCode) {
        log.info("Removing coupon {} from order {}", couponCode, order.getOrderNumber());

        // Find the coupon first
        Coupon coupon = couponRepository.findByCode(couponCode)
            .orElseThrow(() -> new ResourceNotFoundException("Coupon not found: " + couponCode));
        
        OrderCoupon orderCoupon = orderCouponRepository.findByOrderAndCoupon(order, coupon)
            .orElseThrow(() -> new ResourceNotFoundException("Coupon not applied to this order: " + couponCode));

        // Update order totals
        BigDecimal newDiscountAmount = order.getDiscountAmount().subtract(orderCoupon.getDiscountAmount());
        order.setDiscountAmount(newDiscountAmount);
        
        BigDecimal newTotal = order.getSubtotal()
            .add(order.getTaxAmount())
            .add(order.getShippingAmount())
            .subtract(newDiscountAmount);
        order.setTotalAmount(newTotal);

        // Decrement coupon usage
        Coupon coupon = orderCoupon.getCoupon();
        coupon.setTimesUsed(Math.max(0, coupon.getTimesUsed() - 1));
        if (coupon.getStatus() == CouponStatus.USED_UP && coupon.canBeUsed()) {
            coupon.setStatus(CouponStatus.ACTIVE);
        }

        // Save changes
        orderRepository.save(order);
        couponRepository.save(coupon);
        orderCouponRepository.delete(orderCoupon);

        log.info("Successfully removed coupon {} from order {}", couponCode, order.getOrderNumber());
    }

    /**
     * Get coupon by code
     */
    @Transactional(readOnly = true)
    public CouponResponse getCouponByCode(String code) {
        Coupon coupon = couponRepository.findByCode(code)
            .orElseThrow(() -> new ResourceNotFoundException("Coupon not found: " + code));
        return convertToResponse(coupon);
    }

    /**
     * Get all active coupons for a store
     */
    @Transactional(readOnly = true)
    public Page<CouponResponse> getActiveCouponsForStore(Long storeId, Pageable pageable) {
        Store store = storeRepository.findById(storeId)
            .orElseThrow(() -> new ResourceNotFoundException("Store not found: " + storeId));
        
        Page<Coupon> coupons = couponRepository.findByStoreAndStatus(store, CouponStatus.ACTIVE, pageable);
        return coupons.map(this::convertToResponse);
    }

    /**
     * Get all global coupons
     */
    @Transactional(readOnly = true)
    public Page<CouponResponse> getGlobalCoupons(Pageable pageable) {
        Page<Coupon> coupons = couponRepository.findByStoreIsNullAndStatus(CouponStatus.ACTIVE, pageable);
        return coupons.map(this::convertToResponse);
    }

    /**
     * Update coupon statuses (cleanup expired and used up coupons)
     */
    @Transactional
    public void updateCouponStatuses() {
        ZonedDateTime now = ZonedDateTime.now();

        // Mark expired coupons
        couponRepository.findExpiredActiveCoupons(now)
            .forEach(coupon -> {
                coupon.setStatus(CouponStatus.EXPIRED);
                couponRepository.save(coupon);
            });

        // Mark used up coupons
        couponRepository.findUsedUpActiveCoupons()
            .forEach(coupon -> {
                coupon.setStatus(CouponStatus.USED_UP);
                couponRepository.save(coupon);
            });
    }

    // Private helper methods

    private Optional<Coupon> findCouponForStore(String code, Long storeId) {
        Store store = storeRepository.findById(storeId).orElse(null);
        if (store == null) {
            return Optional.empty();
        }
        return couponRepository.findUsableCouponByCodeForStore(code, store, ZonedDateTime.now());
    }

    private String validateCouponUsage(Coupon coupon, CouponValidationRequest request) {
        // Check if coupon can be used
        if (!coupon.canBeUsed()) {
            if (coupon.isExpired()) {
                return "Coupon has expired";
            }
            if (coupon.isUsedUp()) {
                return "Coupon usage limit reached";
            }
            return "Coupon is not active";
        }

        // Check minimum order amount
        if (coupon.getMinimumOrderAmount() != null && 
            request.getOrderAmount().compareTo(coupon.getMinimumOrderAmount()) < 0) {
            return "Order amount must be at least " + coupon.getMinimumOrderAmount() + " " + coupon.getCurrency();
        }

        // Check per-customer usage limit
        if (request.getCustomerEmail() != null && 
            coupon.getUsageLimitPerCustomer() != null && 
            coupon.getUsageLimitPerCustomer() > 0) {
            long customerUsage = couponRepository.countUsageByCustomer(coupon, request.getCustomerEmail());
            if (customerUsage >= coupon.getUsageLimitPerCustomer()) {
                return "You have reached the usage limit for this coupon";
            }
        }

        // Check first-time customer requirement
        if (coupon.getFirstTimeCustomerOnly() && request.getCustomerEmail() != null) {
            boolean hasUsedCoupons = orderCouponRepository.hasCustomerUsedAnyCoupon(request.getCustomerEmail());
            if (hasUsedCoupons) {
                return "This coupon is only available for first-time customers";
            }
        }

        return null; // No validation errors
    }

    private BigDecimal calculateDiscount(Coupon coupon, BigDecimal orderAmount) {
        return coupon.calculateDiscount(orderAmount);
    }

    private CouponResponse convertToResponse(Coupon coupon) {
        return CouponResponse.builder()
            .id(coupon.getId())
            .code(coupon.getCode())
            .name(coupon.getName())
            .description(coupon.getDescription())
            .type(coupon.getType())
            .status(coupon.getStatus())
            .storeId(coupon.getStore() != null ? coupon.getStore().getId() : null)
            .storeName(coupon.getStore() != null ? coupon.getStore().getName() : null)
            .discountPercentage(coupon.getDiscountPercentage())
            .discountAmount(coupon.getDiscountAmount())
            .minimumOrderAmount(coupon.getMinimumOrderAmount())
            .maximumDiscount(coupon.getMaximumDiscount())
            .usageLimit(coupon.getUsageLimit())
            .usageLimitPerCustomer(coupon.getUsageLimitPerCustomer())
            .timesUsed(coupon.getTimesUsed())
            .validFrom(coupon.getValidFrom())
            .validUntil(coupon.getValidUntil())
            .currency(coupon.getCurrency())
            .appliesToSaleItems(coupon.getAppliesToSaleItems())
            .firstTimeCustomerOnly(coupon.getFirstTimeCustomerOnly())
            .isActive(coupon.isActive())
            .isExpired(coupon.isExpired())
            .isUsedUp(coupon.isUsedUp())
            .createdAt(coupon.getCreatedAt())
            .updatedAt(coupon.getUpdatedAt())
            .build();
    }
}