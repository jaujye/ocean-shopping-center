package com.ocean.shopping.dto.shipping;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

/**
 * Package dimensions and weight for shipping calculation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PackageDto {
    
    @NotNull(message = "Weight is required")
    @Positive(message = "Weight must be positive")
    private BigDecimal weight;
    
    @NotNull(message = "Weight unit is required")
    @Size(min = 2, max = 3)
    @Builder.Default
    private String weightUnit = "kg"; // kg or lb
    
    @NotNull(message = "Length is required")
    @Positive(message = "Length must be positive")
    private BigDecimal length;
    
    @NotNull(message = "Width is required")
    @Positive(message = "Width must be positive")
    private BigDecimal width;
    
    @NotNull(message = "Height is required")
    @Positive(message = "Height must be positive")
    private BigDecimal height;
    
    @NotNull(message = "Dimension unit is required")
    @Size(min = 2, max = 3)
    @Builder.Default
    private String dimensionUnit = "cm"; // cm or in
    
    @Size(max = 100)
    private String packageType; // Box, Envelope, etc.
    
    @Size(max = 255)
    private String description;
    
    /**
     * Calculate dimensional weight (volumetric weight)
     * Formula: (L × W × H) / dimensional factor
     * 
     * @param dimensionalFactor The factor used by carrier (e.g., 5000 for DHL)
     * @return Dimensional weight in same unit as dimensions
     */
    public BigDecimal getDimensionalWeight(int dimensionalFactor) {
        BigDecimal volume = length.multiply(width).multiply(height);
        return volume.divide(BigDecimal.valueOf(dimensionalFactor), 2, BigDecimal.ROUND_HALF_UP);
    }
    
    /**
     * Get the chargeable weight (higher of actual weight and dimensional weight)
     * 
     * @param dimensionalFactor The factor used by carrier
     * @return Chargeable weight for shipping calculation
     */
    public BigDecimal getChargeableWeight(int dimensionalFactor) {
        BigDecimal dimensionalWeight = getDimensionalWeight(dimensionalFactor);
        return weight.compareTo(dimensionalWeight) > 0 ? weight : dimensionalWeight;
    }
}