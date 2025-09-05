package com.ocean.shopping.repository;

import com.ocean.shopping.model.entity.Invoice;
import com.ocean.shopping.model.entity.Order;
import com.ocean.shopping.model.entity.enums.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    /**
     * Find invoice by invoice number
     */
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    /**
     * Find invoice by order
     */
    Optional<Invoice> findByOrder(Order order);

    /**
     * Find invoice by order ID
     */
    Optional<Invoice> findByOrder_Id(Long orderId);

    /**
     * Check if invoice exists for order
     */
    boolean existsByOrder(Order order);

    /**
     * Find invoices by status
     */
    Page<Invoice> findByStatus(InvoiceStatus status, Pageable pageable);

    /**
     * Find invoices within date range
     */
    @Query("SELECT i FROM Invoice i WHERE i.generatedAt >= :startDate AND i.generatedAt <= :endDate ORDER BY i.generatedAt DESC")
    Page<Invoice> findInvoicesInDateRange(@Param("startDate") ZonedDateTime startDate, 
                                         @Param("endDate") ZonedDateTime endDate, 
                                         Pageable pageable);

    /**
     * Find invoices that need to be generated (draft status)
     */
    List<Invoice> findByStatus(InvoiceStatus status);

    /**
     * Find invoices by store
     */
    @Query("SELECT i FROM Invoice i WHERE i.order.store.id = :storeId ORDER BY i.createdAt DESC")
    Page<Invoice> findByStoreId(@Param("storeId") Long storeId, Pageable pageable);

    /**
     * Find invoices by customer email
     */
    @Query("SELECT i FROM Invoice i WHERE i.order.customerEmail = :customerEmail ORDER BY i.createdAt DESC")
    Page<Invoice> findByCustomerEmail(@Param("customerEmail") String customerEmail, Pageable pageable);

    /**
     * Count invoices by status
     */
    long countByStatus(InvoiceStatus status);

    /**
     * Find recent invoices
     */
    @Query("SELECT i FROM Invoice i WHERE i.generatedAt >= :since ORDER BY i.generatedAt DESC")
    List<Invoice> findRecentInvoices(@Param("since") ZonedDateTime since);

    /**
     * Find invoices with files
     */
    @Query("SELECT i FROM Invoice i WHERE i.filePath IS NOT NULL AND i.filePath != ''")
    Page<Invoice> findInvoicesWithFiles(Pageable pageable);

    /**
     * Find invoices without files (need generation)
     */
    @Query("SELECT i FROM Invoice i WHERE i.filePath IS NULL OR i.filePath = ''")
    List<Invoice> findInvoicesWithoutFiles();

    /**
     * Get invoice download statistics
     */
    @Query("SELECT " +
           "COUNT(i) as totalInvoices, " +
           "SUM(i.downloadCount) as totalDownloads, " +
           "AVG(i.downloadCount) as avgDownloads, " +
           "MAX(i.downloadCount) as maxDownloads " +
           "FROM Invoice i WHERE i.status != 'DRAFT'")
    Object[] getInvoiceDownloadStats();

    /**
     * Find most downloaded invoices
     */
    @Query("SELECT i FROM Invoice i WHERE i.downloadCount > 0 ORDER BY i.downloadCount DESC")
    Page<Invoice> findMostDownloadedInvoices(Pageable pageable);

    /**
     * Find invoices that haven't been sent yet
     */
    @Query("SELECT i FROM Invoice i WHERE i.status = 'GENERATED' AND i.sentAt IS NULL")
    List<Invoice> findUnsentInvoices();

    /**
     * Get monthly invoice generation statistics
     */
    @Query("SELECT " +
           "EXTRACT(YEAR FROM i.generatedAt) as year, " +
           "EXTRACT(MONTH FROM i.generatedAt) as month, " +
           "COUNT(i) as invoiceCount " +
           "FROM Invoice i " +
           "WHERE i.generatedAt >= :startDate AND i.generatedAt <= :endDate " +
           "GROUP BY EXTRACT(YEAR FROM i.generatedAt), EXTRACT(MONTH FROM i.generatedAt) " +
           "ORDER BY year DESC, month DESC")
    List<Object[]> getMonthlyInvoiceStats(@Param("startDate") ZonedDateTime startDate, 
                                         @Param("endDate") ZonedDateTime endDate);

    /**
     * Find invoices that need cleanup (old files)
     */
    @Query("SELECT i FROM Invoice i WHERE i.createdAt < :cutoffDate AND i.filePath IS NOT NULL")
    List<Invoice> findInvoicesForCleanup(@Param("cutoffDate") ZonedDateTime cutoffDate);

    /**
     * Check if invoice number exists
     */
    boolean existsByInvoiceNumber(String invoiceNumber);

    /**
     * Find next available invoice number pattern
     */
    @Query("SELECT i.invoiceNumber FROM Invoice i WHERE i.invoiceNumber LIKE :pattern ORDER BY i.invoiceNumber DESC")
    List<String> findInvoiceNumbersLike(@Param("pattern") String pattern, Pageable pageable);
}