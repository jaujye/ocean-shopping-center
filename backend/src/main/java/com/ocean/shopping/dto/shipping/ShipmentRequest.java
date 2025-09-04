package com.ocean.shopping.dto.shipping;

import com.ocean.shopping.model.entity.enums.CarrierType;
import com.ocean.shopping.model.entity.enums.ServiceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Request DTO for creating a shipment
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentRequest {
    
    @NotNull(message = "Carrier is required")
    private CarrierType carrier;
    
    @NotNull(message = "Service type is required")
    private ServiceType serviceType;
    
    @NotBlank(message = "Rate ID is required")
    private String rateId; // From rate quote
    
    @NotNull(message = "Shipper address is required")
    @Valid
    private AddressDto shipperAddress;
    
    @NotNull(message = "Recipient address is required")
    @Valid
    private AddressDto recipientAddress;
    
    @NotNull(message = "Packages are required")
    @Size(min = 1, message = "At least one package is required")
    @Valid
    private List<PackageDto> packages;
    
    @Size(max = 100)
    private String orderNumber; // Customer order reference
    
    @Size(max = 255)
    private String reference; // Additional reference
    
    private LocalDate shipDate;
    
    private BigDecimal declaredValue;
    
    private String currency;
    
    @Builder.Default
    private boolean includeInsurance = false;
    
    @Builder.Default
    private boolean signatureRequired = false;
    
    @Builder.Default
    private boolean saturdayDelivery = false;
    
    @Builder.Default
    private boolean adultSignatureRequired = false;
    
    @Builder.Default
    private boolean deliveryConfirmationRequired = true;
    
    private String specialInstructions;
    
    private String deliveryInstructions;
    
    // Customs information for international shipments
    private List<CustomsItemDto> customsItems;
    
    private String reasonForExport; // Gift, Documents, Commercial Sample, etc.
    
    private String termsOfTrade; // DAP, DDP, etc.
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomsItemDto {
        @NotBlank
        private String description;
        
        @NotNull
        private Integer quantity;
        
        @NotNull
        private BigDecimal unitValue;
        
        @NotNull
        private BigDecimal totalValue;
        
        @NotNull
        private BigDecimal weight;
        
        private String countryOfOrigin;
        
        private String hsCode; // Harmonized System code
        
        private String sku;
    }
}