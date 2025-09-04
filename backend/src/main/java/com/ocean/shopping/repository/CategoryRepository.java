package com.ocean.shopping.repository;

import com.ocean.shopping.model.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Category entity
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID>, JpaSpecificationExecutor<Category> {

    // Basic finder methods
    Optional<Category> findBySlug(String slug);

    Optional<Category> findBySlugAndIsActiveTrue(String slug);

    List<Category> findByIsActiveTrueOrderBySortOrder();

    Page<Category> findByIsActiveTrueOrderBySortOrder(Pageable pageable);

    // Parent-child relationships
    List<Category> findByParentIsNullAndIsActiveTrueOrderBySortOrder();

    List<Category> findByParentIdAndIsActiveTrueOrderBySortOrder(UUID parentId);

    Page<Category> findByParentIsNull(Pageable pageable);

    Page<Category> findByParentId(UUID parentId, Pageable pageable);

    // Hierarchy queries
    @Query("SELECT c FROM Category c WHERE c.parent.id = :parentId AND c.isActive = true ORDER BY c.sortOrder")
    List<Category> findActiveSubcategoriesByParent(@Param("parentId") UUID parentId);

    @Query("SELECT c FROM Category c WHERE c.parent IS NULL AND c.isActive = true ORDER BY c.sortOrder")
    List<Category> findActiveRootCategories();

    // Search by name
    @Query("SELECT c FROM Category c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) AND c.isActive = true")
    Page<Category> searchByName(@Param("query") String query, Pageable pageable);

    // Count methods
    long countByParentId(UUID parentId);

    long countByParentIsNull();

    long countByIsActiveTrue();

    @Query("SELECT COUNT(c) FROM Category c WHERE c.parent.id = :parentId AND c.isActive = true")
    long countActiveSubcategoriesByParent(@Param("parentId") UUID parentId);

    // Product count in categories
    @Query("SELECT c FROM Category c LEFT JOIN c.products p WHERE c.isActive = true GROUP BY c.id ORDER BY COUNT(p) DESC")
    List<Category> findCategoriesOrderByProductCount(Pageable pageable);

    // Existence checks
    boolean existsBySlug(String slug);

    boolean existsByParentIdAndSlug(UUID parentId, String slug);

    // Bulk operations
    @Query("SELECT c.id FROM Category c WHERE c.isActive = true")
    List<UUID> findActiveCategoryIds();

    @Query("SELECT c FROM Category c WHERE c.parent.id IN :parentIds AND c.isActive = true")
    List<Category> findActiveSubcategoriesByParents(@Param("parentIds") List<UUID> parentIds);
}