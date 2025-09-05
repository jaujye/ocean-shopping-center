package com.ocean.shopping.dto.shipping;

import lombok.*;

import java.util.List;

/**
 * Response DTO for address validation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressValidationResponse {
    
    private Boolean isValid;
    
    private AddressDto originalAddress;
    
    private AddressDto correctedAddress;
    
    private List<String> validationMessages;
    
    private List<String> validationWarnings;
    
    private List<String> validationErrors;
    
    private String validationStatus; // VALID, CORRECTED, INVALID, AMBIGUOUS
    
    private List<AddressDto> suggestedAddresses; // Multiple suggestions for ambiguous addresses
    
    private Double confidenceScore; // 0.0 to 1.0
    
    private Boolean isResidential; // Residential vs commercial address
    
    private Boolean isDeliverable; // Whether carrier can deliver to this address
    
    private String carrierRoute; // Postal service route information
    
    /**
     * Check if address was corrected
     */
    public boolean wasAddressCorrected() {
        return correctedAddress != null && !correctedAddress.equals(originalAddress);
    }
    
    /**
     * Get the best address to use (corrected if available, original otherwise)
     */
    public AddressDto getBestAddress() {
        return correctedAddress != null ? correctedAddress : originalAddress;
    }
    
    /**
     * Check if validation was successful (valid or corrected)
     */
    public boolean isValidationSuccessful() {
        return Boolean.TRUE.equals(isValid) || correctedAddress != null;
    }
}