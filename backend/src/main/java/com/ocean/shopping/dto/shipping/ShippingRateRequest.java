package com.ocean.shopping.dto.shipping;

import com.ocean.shopping.model.entity.enums.CarrierType;
import com.ocean.shopping.model.entity.enums.ServiceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Request DTO for shipping rate calculation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingRateRequest {
    
    @NotNull(message = "Origin address is required")
    @Valid
    private AddressDto originAddress;
    
    @NotNull(message = "Destination address is required")
    @Valid
    private AddressDto destinationAddress;
    
    @NotNull(message = "Packages are required")
    @Size(min = 1, message = "At least one package is required")
    @Valid
    private List<PackageDto> packages;
    
    private List<CarrierType> preferredCarriers; // null means all carriers
    
    private List<ServiceType> preferredServices; // null means all services
    
    private LocalDate shipDate; // null means today
    
    private BigDecimal declaredValue; // for insurance calculation
    
    private String currency; // default USD
    
    @Builder.Default
    private boolean includeDuties = false; // for international shipments
    
    @Builder.Default
    private boolean includeInsurance = false;
    
    @Builder.Default
    private boolean signatureRequired = false;
    
    @Builder.Default
    private boolean saturdayDelivery = false;
    
    private String specialInstructions;
    
    /**
     * Get total weight of all packages
     */
    public BigDecimal getTotalWeight() {
        return packages.stream()
                .map(PackageDto::getWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Check if shipment is international
     */
    public boolean isInternational() {
        return !originAddress.getCountry().equalsIgnoreCase(destinationAddress.getCountry());
    }
}