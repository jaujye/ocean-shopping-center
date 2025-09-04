package com.ocean.shopping.service;

import com.ocean.shopping.exception.ResourceNotFoundException;
import com.ocean.shopping.model.entity.Store;
import com.ocean.shopping.model.entity.enums.StoreStatus;
import com.ocean.shopping.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Store service for managing stores
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StoreService {

    private final StoreRepository storeRepository;

    /**
     * Get store by ID
     */
    @Transactional(readOnly = true)
    public Store getStoreById(UUID id) {
        log.debug("Getting store by ID: {}", id);
        return storeRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forEntity("Store", id));
    }

    /**
     * Get active store by ID
     */
    @Transactional(readOnly = true)
    public Store getActiveStoreById(UUID id) {
        log.debug("Getting active store by ID: {}", id);
        Store store = getStoreById(id);
        if (!store.isActive()) {
            throw new ResourceNotFoundException("Store is not active");
        }
        return store;
    }

    /**
     * Get store by slug
     */
    @Transactional(readOnly = true)
    public Store getStoreBySlug(String slug) {
        log.debug("Getting store by slug: {}", slug);
        return storeRepository.findBySlug(slug)
                .orElseThrow(() -> ResourceNotFoundException.forField("Store", "slug", slug));
    }

    /**
     * Get active store by slug
     */
    @Transactional(readOnly = true)
    public Store getActiveStoreBySlug(String slug) {
        log.debug("Getting active store by slug: {}", slug);
        return storeRepository.findBySlugAndStatus(slug, StoreStatus.ACTIVE)
                .orElseThrow(() -> ResourceNotFoundException.forField("Store", "slug", slug));
    }

    /**
     * Validate user has access to store
     */
    @Transactional(readOnly = true)
    public void validateStoreAccess(UUID storeId, UUID userId) {
        Store store = getStoreById(storeId);
        
        // Check if user is the owner of the store
        if (!store.getOwner().getId().equals(userId)) {
            throw new ResourceNotFoundException("User does not have access to this store");
        }
    }

    /**
     * Check if store exists and is active
     */
    @Transactional(readOnly = true)
    public boolean isStoreActive(UUID storeId) {
        return storeRepository.findById(storeId)
                .map(Store::isActive)
                .orElse(false);
    }
}