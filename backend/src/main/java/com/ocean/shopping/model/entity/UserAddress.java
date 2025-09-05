package com.ocean.shopping.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * User address entity for shipping and billing addresses
 */
@Entity
@Table(name = "user_addresses", indexes = {
    @Index(name = "idx_user_addresses_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAddress extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "label", nullable = false)
    @Size(max = 50)
    @Builder.Default
    private String label = "Home";

    @Column(name = "first_name", nullable = false)
    @NotBlank(message = "First name is required")
    @Size(max = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    @NotBlank(message = "Last name is required")
    @Size(max = 100)
    private String lastName;

    @Column(name = "company")
    @Size(max = 255)
    private String company;

    @Column(name = "address_line_1", nullable = false)
    @NotBlank(message = "Address line 1 is required")
    @Size(max = 255)
    private String addressLine1;

    @Column(name = "address_line_2")
    @Size(max = 255)
    private String addressLine2;

    @Column(name = "city", nullable = false)
    @NotBlank(message = "City is required")
    @Size(max = 100)
    private String city;

    @Column(name = "state_province")
    @Size(max = 100)
    private String stateProvince;

    @Column(name = "postal_code", nullable = false)
    @NotBlank(message = "Postal code is required")
    @Size(max = 20)
    private String postalCode;

    @Column(name = "country", nullable = false)
    @Size(min = 2, max = 2)
    @Builder.Default
    private String country = "US";

    @Column(name = "phone")
    @Size(max = 20)
    private String phone;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    // Helper methods
    public String getFullName() {
        return firstName + " " + lastName;
    }

    public String getFullAddress() {
        StringBuilder address = new StringBuilder();
        address.append(addressLine1);
        if (addressLine2 != null && !addressLine2.trim().isEmpty()) {
            address.append(", ").append(addressLine2);
        }
        address.append(", ").append(city);
        if (stateProvince != null && !stateProvince.trim().isEmpty()) {
            address.append(", ").append(stateProvince);
        }
        address.append(" ").append(postalCode);
        address.append(", ").append(country);
        return address.toString();
    }
}