package com.ocean.shopping.dto.shipping;

import com.ocean.shopping.model.entity.enums.CarrierType;
import com.ocean.shopping.model.entity.enums.ServiceType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for shipping rate calculation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingRateResponse {
    
    private CarrierType carrier;
    
    private ServiceType serviceType;
    
    private String serviceName;
    
    private String serviceDescription;
    
    private BigDecimal baseRate;
    
    private BigDecimal fuelSurcharge;
    
    private BigDecimal additionalFees;
    
    private BigDecimal totalCost;
    
    private String currency;
    
    private LocalDate estimatedDeliveryDate;
    
    private LocalDateTime estimatedDeliveryTime;
    
    private Integer transitDays;
    
    private Boolean guaranteedDelivery;
    
    private List<String> features; // Tracking, Insurance, etc.
    
    private Map<String, BigDecimal> feeBreakdown; // Detailed fee structure
    
    private String rateId; // Unique identifier for this rate quote
    
    private LocalDateTime quoteExpires; // When this quote expires
    
    private Boolean available; // Whether service is available for this route
    
    private String unavailableReason; // Reason if not available
    
    private Map<String, String> carrierMetadata; // Carrier-specific data
    
    /**
     * Check if delivery is guaranteed by a specific time
     */
    public boolean hasTimeGuarantee() {
        return estimatedDeliveryTime != null && Boolean.TRUE.equals(guaranteedDelivery);
    }
    
    /**
     * Get formatted delivery estimate
     */
    public String getDeliveryEstimate() {
        if (estimatedDeliveryDate == null) {
            return transitDays != null ? transitDays + " business days" : "Unknown";
        }
        
        StringBuilder estimate = new StringBuilder();
        estimate.append(estimatedDeliveryDate.toString());
        
        if (estimatedDeliveryTime != null) {
            estimate.append(" by ").append(estimatedDeliveryTime.toLocalTime().toString());
        }
        
        return estimate.toString();
    }
    
    /**
     * Check if quote is still valid
     */
    public boolean isQuoteValid() {
        return quoteExpires == null || LocalDateTime.now().isBefore(quoteExpires);
    }
}