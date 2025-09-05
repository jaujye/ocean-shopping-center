package com.ocean.shopping.repository;

import com.ocean.shopping.model.entity.Store;
import com.ocean.shopping.model.entity.enums.StoreStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Store entity
 */
@Repository
public interface StoreRepository extends JpaRepository<Store, UUID>, JpaSpecificationExecutor<Store> {

    // Basic finder methods
    Optional<Store> findBySlug(String slug);

    Optional<Store> findBySlugAndStatus(String slug, StoreStatus status);

    List<Store> findByOwnerId(UUID ownerId);

    List<Store> findByOwnerIdAndStatus(UUID ownerId, StoreStatus status);

    Page<Store> findByStatus(StoreStatus status, Pageable pageable);

    List<Store> findByStatusOrderByCreatedAtDesc(StoreStatus status);

    // Active stores
    @Query("SELECT s FROM Store s WHERE s.status = 'ACTIVE'")
    List<Store> findActiveStores();

    @Query("SELECT s FROM Store s WHERE s.status = 'ACTIVE'")
    Page<Store> findActiveStores(Pageable pageable);

    // Search by name
    @Query("SELECT s FROM Store s WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%')) AND s.status = 'ACTIVE'")
    Page<Store> searchByName(@Param("query") String query, Pageable pageable);

    // Recently created stores
    @Query("SELECT s FROM Store s WHERE s.createdAt >= :since ORDER BY s.createdAt DESC")
    List<Store> findRecentStores(@Param("since") ZonedDateTime since, Pageable pageable);

    // Count methods
    long countByStatus(StoreStatus status);

    long countByOwnerId(UUID ownerId);

    // Existence checks
    boolean existsBySlug(String slug);

    boolean existsByOwnerIdAndSlug(UUID ownerId, String slug);

    // Analytics
    @Query("SELECT COUNT(s) FROM Store s WHERE s.status = 'ACTIVE' AND s.createdAt >= :since")
    long countNewActiveStores(@Param("since") ZonedDateTime since);
}