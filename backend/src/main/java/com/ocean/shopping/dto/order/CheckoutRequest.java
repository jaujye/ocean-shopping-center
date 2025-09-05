package com.ocean.shopping.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for checkout request containing billing, shipping, and payment information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRequest {

    @NotBlank(message = "Customer email is required")
    @Email(message = "Please provide a valid email address")
    @Size(max = 255)
    private String customerEmail;

    @Size(max = 20)
    private String customerPhone;

    @Valid
    @NotNull(message = "Billing address is required")
    private AddressDto billingAddress;

    @Valid
    @NotNull(message = "Shipping address is required")
    private AddressDto shippingAddress;

    @NotBlank(message = "Payment method ID is required")
    private String paymentMethodId;

    private String couponCode;

    @Size(max = 1000)
    private String notes;

    private boolean savePaymentMethod = false;

    // Flag to indicate if billing and shipping addresses are the same
    private boolean sameAsShipping = false;

    /**
     * Address DTO for billing and shipping
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressDto {
        
        @NotBlank(message = "First name is required")
        @Size(max = 100)
        private String firstName;

        @NotBlank(message = "Last name is required")
        @Size(max = 100)
        private String lastName;

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
    }
}