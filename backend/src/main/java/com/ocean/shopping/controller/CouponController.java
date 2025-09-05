package com.ocean.shopping.controller;

import com.ocean.shopping.dto.coupon.CouponResponse;
import com.ocean.shopping.dto.coupon.CouponValidationRequest;
import com.ocean.shopping.dto.coupon.CouponValidationResponse;
import com.ocean.shopping.exception.ErrorResponse;
import com.ocean.shopping.service.CouponService;
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
import org.springframework.web.bind.annotation.*;

/**
 * Coupon validation and management controller
 */
@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Coupon Management", description = "Coupon validation and management endpoints")
public class CouponController {

    private final CouponService couponService;

    @Operation(summary = "Validate coupon code", 
               description = "Validate a coupon code and calculate discount amount")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Coupon validation result",
                    content = @Content(schema = @Schema(implementation = CouponValidationResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Coupon not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/validate")
    public ResponseEntity<CouponValidationResponse> validateCoupon(
            @Valid @RequestBody CouponValidationRequest request) {
        log.info("Validating coupon: {}", request.getCode());
        
        CouponValidationResponse response = couponService.validateCoupon(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get coupon by code", 
               description = "Retrieve detailed information about a coupon by its code")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Coupon found",
                    content = @Content(schema = @Schema(implementation = CouponResponse.class))),
        @ApiResponse(responseCode = "404", description = "Coupon not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{code}")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_OWNER')")
    public ResponseEntity<CouponResponse> getCouponByCode(
            @Parameter(description = "Coupon code", example = "SAVE20")
            @PathVariable String code) {
        log.info("Retrieving coupon by code: {}", code);
        
        CouponResponse response = couponService.getCouponByCode(code);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get global coupons", 
               description = "Retrieve all active global coupons (not store-specific)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Global coupons retrieved",
                    content = @Content(schema = @Schema(implementation = Page.class)))
    })
    @GetMapping("/global")
    public ResponseEntity<Page<CouponResponse>> getGlobalCoupons(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        log.info("Retrieving global coupons");
        
        Page<CouponResponse> coupons = couponService.getGlobalCoupons(pageable);
        return ResponseEntity.ok(coupons);
    }

    @Operation(summary = "Get store coupons", 
               description = "Retrieve all active coupons for a specific store")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Store coupons retrieved",
                    content = @Content(schema = @Schema(implementation = Page.class))),
        @ApiResponse(responseCode = "404", description = "Store not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/store/{storeId}")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_OWNER')")
    public ResponseEntity<Page<CouponResponse>> getStoreCoupons(
            @Parameter(description = "Store ID", example = "1")
            @PathVariable Long storeId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        log.info("Retrieving coupons for store: {}", storeId);
        
        Page<CouponResponse> coupons = couponService.getActiveCouponsForStore(storeId, pageable);
        return ResponseEntity.ok(coupons);
    }

    @Operation(summary = "Update coupon statuses", 
               description = "Update expired and used up coupon statuses (admin only)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Coupon statuses updated"),
        @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/update-statuses")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateCouponStatuses() {
        log.info("Updating coupon statuses");
        
        couponService.updateCouponStatuses();
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Quick validate coupon", 
               description = "Quick validation endpoint for simple coupon code check")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Coupon is valid"),
        @ApiResponse(responseCode = "400", description = "Coupon is invalid"),
        @ApiResponse(responseCode = "404", description = "Coupon not found")
    })
    @GetMapping("/quick-validate/{code}")
    public ResponseEntity<Boolean> quickValidate(
            @Parameter(description = "Coupon code to validate", example = "SAVE20")
            @PathVariable String code,
            @Parameter(description = "Order amount for validation", example = "100.00")
            @RequestParam(required = false, defaultValue = "0") String orderAmount,
            @Parameter(description = "Store ID for store-specific validation", example = "1")
            @RequestParam(required = false) Long storeId) {
        
        try {
            CouponValidationRequest request = new CouponValidationRequest();
            request.setCode(code);
            request.setOrderAmount(new java.math.BigDecimal(orderAmount));
            request.setStoreId(storeId);
            
            CouponValidationResponse response = couponService.validateCoupon(request);
            return ResponseEntity.ok(response.isValid());
            
        } catch (Exception e) {
            log.debug("Quick validation failed for coupon {}: {}", code, e.getMessage());
            return ResponseEntity.ok(false);
        }
    }

    @Operation(summary = "Get coupon discount preview", 
               description = "Preview discount amount without applying the coupon")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Discount preview calculated",
                    content = @Content(schema = @Schema(implementation = CouponValidationResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{code}/preview")
    public ResponseEntity<CouponValidationResponse> getDiscountPreview(
            @Parameter(description = "Coupon code", example = "SAVE20")
            @PathVariable String code,
            @Parameter(description = "Order amount", example = "100.00")
            @RequestParam java.math.BigDecimal orderAmount,
            @Parameter(description = "Store ID", example = "1")
            @RequestParam(required = false) Long storeId,
            @Parameter(description = "Customer email", example = "customer@example.com")
            @RequestParam(required = false) String customerEmail) {
        
        log.info("Getting discount preview for coupon: {} with order amount: {}", code, orderAmount);
        
        CouponValidationRequest request = new CouponValidationRequest();
        request.setCode(code);
        request.setOrderAmount(orderAmount);
        request.setStoreId(storeId);
        request.setCustomerEmail(customerEmail);
        
        CouponValidationResponse response = couponService.validateCoupon(request);
        return ResponseEntity.ok(response);
    }
}