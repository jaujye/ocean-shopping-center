package com.ocean.shopping.service;

import com.ocean.shopping.exception.ResourceNotFoundException;
import com.ocean.shopping.model.entity.Category;
import com.ocean.shopping.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Category service for managing categories
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /**
     * Get category by ID
     */
    @Transactional(readOnly = true)
    public Category getCategoryById(UUID id) {
        log.debug("Getting category by ID: {}", id);
        return categoryRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forEntity("Category", id));
    }

    /**
     * Get active category by ID
     */
    @Transactional(readOnly = true)
    public Category getActiveCategoryById(UUID id) {
        log.debug("Getting active category by ID: {}", id);
        Category category = getCategoryById(id);
        if (!category.getIsActive()) {
            throw new ResourceNotFoundException("Category is not active");
        }
        return category;
    }

    /**
     * Get category by slug
     */
    @Transactional(readOnly = true)
    public Category getCategoryBySlug(String slug) {
        log.debug("Getting category by slug: {}", slug);
        return categoryRepository.findBySlug(slug)
                .orElseThrow(() -> ResourceNotFoundException.forField("Category", "slug", slug));
    }

    /**
     * Get active category by slug
     */
    @Transactional(readOnly = true)
    public Category getActiveCategoryBySlug(String slug) {
        log.debug("Getting active category by slug: {}", slug);
        return categoryRepository.findBySlugAndIsActiveTrue(slug)
                .orElseThrow(() -> ResourceNotFoundException.forField("Category", "slug", slug));
    }

    /**
     * Check if category exists and is active
     */
    @Transactional(readOnly = true)
    public boolean isCategoryActive(UUID categoryId) {
        return categoryRepository.findById(categoryId)
                .map(Category::getIsActive)
                .orElse(false);
    }
}