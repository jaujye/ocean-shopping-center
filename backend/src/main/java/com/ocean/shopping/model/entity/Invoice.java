package com.ocean.shopping.model.entity;

import com.ocean.shopping.model.entity.enums.InvoiceStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.ZonedDateTime;

/**
 * Invoice entity for PDF generation and tracking
 */
@Entity
@Table(name = "invoices", indexes = {
    @Index(name = "idx_invoices_invoice_number", columnList = "invoice_number", unique = true),
    @Index(name = "idx_invoices_order_id", columnList = "order_id"),
    @Index(name = "idx_invoices_status", columnList = "status"),
    @Index(name = "idx_invoices_generated_at", columnList = "generated_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice extends BaseEntity {

    @Column(name = "invoice_number", unique = true, nullable = false)
    @NotBlank(message = "Invoice number is required")
    @Size(max = 50)
    private String invoiceNumber;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    @NotNull(message = "Order is required")
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @Column(name = "file_path")
    @Size(max = 500)
    private String filePath;

    @Column(name = "file_name")
    @Size(max = 255)
    private String fileName;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "generated_at")
    private ZonedDateTime generatedAt;

    @Column(name = "sent_at")
    private ZonedDateTime sentAt;

    @Column(name = "download_count")
    @Builder.Default
    private Integer downloadCount = 0;

    @Column(name = "last_downloaded_at")
    private ZonedDateTime lastDownloadedAt;

    // Helper methods
    public boolean isGenerated() {
        return status == InvoiceStatus.GENERATED || status == InvoiceStatus.SENT || status == InvoiceStatus.PAID;
    }

    public boolean hasFile() {
        return filePath != null && !filePath.trim().isEmpty();
    }

    public void markAsGenerated(String filePath, String fileName, Long fileSize) {
        this.status = InvoiceStatus.GENERATED;
        this.filePath = filePath;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.generatedAt = ZonedDateTime.now();
    }

    public void markAsSent() {
        if (isGenerated()) {
            this.status = InvoiceStatus.SENT;
            this.sentAt = ZonedDateTime.now();
        }
    }

    public void incrementDownloadCount() {
        this.downloadCount++;
        this.lastDownloadedAt = ZonedDateTime.now();
    }

    public String generateInvoiceNumber() {
        if (order != null) {
            return "INV-" + order.getOrderNumber();
        }
        return "INV-" + System.currentTimeMillis();
    }
}