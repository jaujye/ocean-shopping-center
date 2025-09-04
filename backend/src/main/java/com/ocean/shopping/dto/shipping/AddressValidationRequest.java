package com.ocean.shopping.dto.shipping;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Request DTO for address validation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressValidationRequest {
    
    @Size(max = 100)
    private String name;
    
    @Size(max = 100)
    private String company;
    
    @NotBlank(message = "Address line 1 is required")
    @Size(max = 255)
    private String addressLine1;
    
    @Size(max = 255)
    private String addressLine2;
    
    @NotBlank(message = "City is required")
    @Size(max = 100)
    private String city;
    
    @Size(max = 100)
    private String state;
    
    @NotBlank(message = "Postal code is required")
    @Size(max = 20)
    private String postalCode;
    
    @NotBlank(message = "Country is required")
    @Size(min = 2, max = 2)
    private String country;
    
    @Size(max = 20)
    private String phone;
}