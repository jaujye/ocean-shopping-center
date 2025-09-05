package com.ocean.shopping.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.Valid;

/**
 * Configuration properties for logistics and shipping
 */
@ConfigurationProperties(prefix = "logistics")
@Data
@Validated
public class LogisticsProperties {

    /**
     * Global logistics settings
     */
    private boolean enabled = true;
    
    /**
     * Rate shopping settings
     */
    @Valid
    private RateShoppingProperties rateShopping = new RateShoppingProperties();
    
    /**
     * Tracking settings
     */
    @Valid
    private TrackingProperties tracking = new TrackingProperties();
    
    /**
     * Webhook settings
     */
    @Valid
    private WebhookProperties webhook = new WebhookProperties();
    
    /**
     * DHL carrier configuration
     */
    @Valid
    private DhlProperties dhl = new DhlProperties();
    
    /**
     * FedEx carrier configuration
     */
    @Valid
    private FedexProperties fedex = new FedexProperties();
    
    /**
     * UPS carrier configuration
     */
    @Valid
    private UpsProperties ups = new UpsProperties();
    
    /**
     * USPS carrier configuration
     */
    @Valid
    private UspsProperties usps = new UspsProperties();

    @Data
    public static class RateShoppingProperties {
        private boolean enabled = true;
        
        @Positive
        private int timeoutSeconds = 30;
        
        @Positive
        private int maxRetries = 3;
        
        private boolean includeDisabledCarriers = false;
        
        private boolean cacheRates = true;
        
        @Positive
        private int cacheExpiryMinutes = 60;
    }

    @Data
    public static class TrackingProperties {
        private boolean enabled = true;
        
        @Positive
        private int updateIntervalMinutes = 60;
        
        @Positive
        private int batchSize = 10;
        
        @Positive
        private int timeoutSeconds = 30;
        
        @Positive
        private int maxRetries = 3;
        
        private boolean enableNotifications = true;
        
        private boolean enableScheduledUpdates = true;
        
        @Positive
        private int retentionDays = 730; // 2 years
    }

    @Data
    public static class WebhookProperties {
        private boolean enabled = true;
        
        @NotBlank
        private String baseUrl = "https://api.oceanshoppingcenter.com";
        
        @NotBlank
        private String webhookPath = "/api/webhooks/tracking";
        
        @NotBlank
        private String secretKey = "change-me-in-production";
        
        @Positive
        private int timeoutSeconds = 30;
        
        private boolean validateSignature = true;
        
        private boolean enableRetryOnFailure = true;
        
        @Positive
        private int maxRetries = 5;
    }

    @Data
    public static class DhlProperties {
        private boolean enabled = false;
        
        private boolean sandbox = true;
        
        private String apiKey;
        
        private String apiSecret;
        
        private String accountNumber;
        
        private String baseUrl = "https://api-sandbox.dhl.com";
        
        private String productionUrl = "https://api.dhl.com";
        
        @Positive
        private int timeoutSeconds = 30;
        
        @Positive
        private int maxRetries = 3;
        
        @Positive
        private int rateLimitPerMinute = 100;
        
        // DHL specific settings
        private String defaultService = "EXPRESS WORLDWIDE";
        
        private String defaultCurrency = "USD";
        
        private boolean enableInsurance = true;
        
        private boolean enableSignature = false;
    }

    @Data
    public static class FedexProperties {
        private boolean enabled = false;
        
        private boolean sandbox = true;
        
        private String apiKey;
        
        private String apiSecret;
        
        private String accountNumber;
        
        private String meterNumber;
        
        private String baseUrl = "https://apis-sandbox.fedex.com";
        
        private String productionUrl = "https://apis.fedex.com";
        
        @Positive
        private int timeoutSeconds = 30;
        
        @Positive
        private int maxRetries = 3;
        
        @Positive
        private int rateLimitPerMinute = 200;
        
        // FedEx specific settings
        private String defaultService = "FEDEX_GROUND";
        
        private String defaultCurrency = "USD";
        
        private boolean enableInsurance = true;
        
        private boolean enableSignature = false;
        
        private boolean enableSaturdayDelivery = false;
    }

    @Data
    public static class UpsProperties {
        private boolean enabled = false;
        
        private boolean sandbox = true;
        
        private String clientId;
        
        private String clientSecret;
        
        private String accountNumber;
        
        private String accessLicenseNumber;
        
        private String baseUrl = "https://onlinetools.ups.com/sandbox";
        
        private String productionUrl = "https://onlinetools.ups.com";
        
        @Positive
        private int timeoutSeconds = 30;
        
        @Positive
        private int maxRetries = 3;
        
        @Positive
        private int rateLimitPerMinute = 150;
        
        // UPS specific settings
        private String defaultService = "UPS_GROUND";
        
        private String defaultCurrency = "USD";
        
        private boolean enableInsurance = true;
        
        private boolean enableSignature = false;
        
        private boolean enableSaturdayDelivery = false;
        
        private String shipperNumber;
    }

    @Data
    public static class UspsProperties {
        private boolean enabled = false;
        
        private boolean sandbox = true;
        
        private String userId;
        
        private String password;
        
        private String apiKey;
        
        private String baseUrl = "https://stg-secure.shippingapis.com";
        
        private String productionUrl = "https://secure.shippingapis.com";
        
        @Positive
        private int timeoutSeconds = 30;
        
        @Positive
        private int maxRetries = 3;
        
        @Positive
        private int rateLimitPerMinute = 100;
        
        // USPS specific settings
        private String defaultService = "PRIORITY";
        
        private String defaultCurrency = "USD";
        
        private boolean enableInsurance = false;
        
        private boolean enableSignature = false;
    }
}