package com.ocean.shopping.config;

import com.ocean.shopping.external.logistics.DhlCarrierService;
import com.ocean.shopping.external.logistics.FedexCarrierService;
import com.ocean.shopping.external.logistics.UpsCarrierService;
import com.ocean.shopping.external.logistics.UspsCarrierService;
import com.ocean.shopping.model.entity.enums.CarrierType;
import com.ocean.shopping.service.logistics.CarrierService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.List;

/**
 * Configuration for logistics and shipping services
 */
@Configuration
@EnableConfigurationProperties(LogisticsProperties.class)
@EnableAsync
@EnableScheduling
@RequiredArgsConstructor
public class LogisticsConfig {

    private final LogisticsProperties properties;

    /**
     * Configure carrier services map
     */
    @Bean
    public Map<CarrierType, CarrierService> carrierServices(
            DhlCarrierService dhlService,
            FedexCarrierService fedexService,
            UpsCarrierService upsService,
            UspsCarrierService uspsService) {
        
        Map<CarrierType, CarrierService> services = Map.of(
            CarrierType.DHL, dhlService,
            CarrierType.FEDEX, fedexService,
            CarrierType.UPS, upsService,
            CarrierType.USPS, uspsService
        );

        // Filter to only enabled carriers
        return services.entrySet().stream()
                .filter(entry -> isCarrierEnabled(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Check if carrier is enabled
     */
    private boolean isCarrierEnabled(CarrierType carrier) {
        switch (carrier) {
            case DHL:
                return properties.getDhl().isEnabled();
            case FEDEX:
                return properties.getFedex().isEnabled();
            case UPS:
                return properties.getUps().isEnabled();
            case USPS:
                return properties.getUsps().isEnabled();
            default:
                return false;
        }
    }

    /**
     * DHL carrier service
     */
    @Bean
    public DhlCarrierService dhlCarrierService() {
        return new DhlCarrierService(properties.getDhl());
    }

    /**
     * FedEx carrier service
     */
    @Bean
    public FedexCarrierService fedexCarrierService() {
        return new FedexCarrierService(properties.getFedex());
    }

    /**
     * UPS carrier service
     */
    @Bean
    public UpsCarrierService upsCarrierService() {
        return new UpsCarrierService(properties.getUps());
    }

    /**
     * USPS carrier service
     */
    @Bean
    public UspsCarrierService uspsCarrierService() {
        return new UspsCarrierService(properties.getUsps());
    }
}