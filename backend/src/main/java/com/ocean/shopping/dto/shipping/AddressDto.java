package com.ocean.shopping.dto.shipping;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Address DTO for shipping operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressDto {
    
    @NotBlank(message = "First name is required")
    @Size(max = 100)
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    @Size(max = 100)
    private String lastName;
    
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
    
    @Size(max = 255)
    private String email;
    
    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();
        sb.append(addressLine1);
        if (addressLine2 != null && !addressLine2.trim().isEmpty()) {
            sb.append(", ").append(addressLine2);
        }
        sb.append(", ").append(city);
        if (state != null && !state.trim().isEmpty()) {
            sb.append(", ").append(state);
        }
        sb.append(" ").append(postalCode);
        sb.append(", ").append(country);
        return sb.toString();
    }
}