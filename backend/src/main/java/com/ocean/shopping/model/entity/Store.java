package com.ocean.shopping.model.entity;

import com.ocean.shopping.model.entity.enums.StoreStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Store entity for multi-tenant management
 */
@Entity
@Table(name = "stores", indexes = {
    @Index(name = "idx_stores_owner_id", columnList = "owner_id"),
    @Index(name = "idx_stores_slug", columnList = "slug"),
    @Index(name = "idx_stores_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Store extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "name", nullable = false)
    @NotBlank(message = "Store name is required")
    @Size(max = 255)
    private String name;

    @Column(name = "slug", unique = true, nullable = false)
    @NotBlank(message = "Store slug is required")
    @Size(max = 255)
    private String slug;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "logo_url")
    @Size(max = 500)
    private String logoUrl;

    @Column(name = "banner_url")
    @Size(max = 500)
    private String bannerUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private StoreStatus status = StoreStatus.PENDING_APPROVAL;

    @Column(name = "business_license")
    @Size(max = 255)
    private String businessLicense;

    @Column(name = "tax_id")
    @Size(max = 50)
    private String taxId;

    @Column(name = "phone")
    @Size(max = 20)
    private String phone;

    @Column(name = "email")
    @Size(max = 255)
    private String email;

    @Column(name = "website_url")
    @Size(max = 500)
    private String websiteUrl;

    @Column(name = "address_line_1")
    @Size(max = 255)
    private String addressLine1;

    @Column(name = "address_line_2")
    @Size(max = 255)
    private String addressLine2;

    @Column(name = "city")
    @Size(max = 100)
    private String city;

    @Column(name = "state_province")
    @Size(max = 100)
    private String stateProvince;

    @Column(name = "postal_code")
    @Size(max = 20)
    private String postalCode;

    @Column(name = "country", nullable = false)
    @Size(min = 2, max = 2)
    @Builder.Default
    private String country = "US";

    @Column(name = "timezone", nullable = false)
    @Size(max = 50)
    @Builder.Default
    private String timezone = "UTC";

    @Column(name = "currency", nullable = false)
    @Size(min = 3, max = 3)
    @Builder.Default
    private String currency = "USD";

    @Column(name = "commission_rate", nullable = false, precision = 5, scale = 4)
    @DecimalMin(value = "0.0000", message = "Commission rate must be positive")
    @DecimalMax(value = "1.0000", message = "Commission rate cannot exceed 100%")
    @Builder.Default
    private BigDecimal commissionRate = new BigDecimal("0.0500"); // 5%

    // Relationships
    @OneToMany(mappedBy = "store", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Product> products;

    @OneToMany(mappedBy = "store", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Order> orders;

    // Helper methods
    public boolean isActive() {
        return status == StoreStatus.ACTIVE;
    }

    public boolean isApproved() {
        return status == StoreStatus.ACTIVE || status == StoreStatus.INACTIVE;
    }

    public String getFullAddress() {
        if (addressLine1 == null || addressLine1.trim().isEmpty()) {
            return null;
        }
        
        StringBuilder address = new StringBuilder();
        address.append(addressLine1);
        if (addressLine2 != null && !addressLine2.trim().isEmpty()) {
            address.append(", ").append(addressLine2);
        }
        if (city != null && !city.trim().isEmpty()) {
            address.append(", ").append(city);
        }
        if (stateProvince != null && !stateProvince.trim().isEmpty()) {
            address.append(", ").append(stateProvince);
        }
        if (postalCode != null && !postalCode.trim().isEmpty()) {
            address.append(" ").append(postalCode);
        }
        address.append(", ").append(country);
        return address.toString();
    }
}