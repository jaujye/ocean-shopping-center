package com.ocean.shopping.service;

import com.ocean.shopping.exception.BadRequestException;
import com.ocean.shopping.exception.ResourceNotFoundException;
import com.ocean.shopping.model.entity.*;
import com.ocean.shopping.model.entity.enums.InvoiceStatus;
import com.ocean.shopping.repository.InvoiceRepository;
import com.ocean.shopping.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Service for invoice generation and management
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final OrderRepository orderRepository;
    private final NotificationService notificationService;

    @Value("${invoice.storage-path:./invoices}")
    private String invoiceStoragePath;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    /**
     * Generate invoice for an order
     */
    @Transactional
    public Invoice generateInvoice(Long orderId) {
        log.info("Generating invoice for order ID: {}", orderId);

        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        // Check if invoice already exists
        if (invoiceRepository.existsByOrder(order)) {
            throw new BadRequestException("Invoice already exists for this order");
        }

        try {
            // Create invoice record
            String invoiceNumber = generateInvoiceNumber(order);
            Invoice invoice = Invoice.builder()
                .invoiceNumber(invoiceNumber)
                .order(order)
                .status(InvoiceStatus.DRAFT)
                .build();

            invoice = invoiceRepository.save(invoice);

            // Generate PDF
            String fileName = invoiceNumber + ".pdf";
            String filePath = generatePDFInvoice(order, invoice, fileName);
            
            // Update invoice with file information
            Path file = Paths.get(filePath);
            invoice.markAsGenerated(filePath, fileName, Files.size(file));
            invoice = invoiceRepository.save(invoice);

            log.info("Successfully generated invoice {} for order {}", invoiceNumber, order.getOrderNumber());
            return invoice;

        } catch (Exception e) {
            log.error("Error generating invoice for order {}: {}", order.getOrderNumber(), e.getMessage(), e);
            throw new BadRequestException("Failed to generate invoice: " + e.getMessage());
        }
    }

    /**
     * Get invoice by order ID
     */
    @Transactional(readOnly = true)
    public Invoice getInvoiceByOrderId(Long orderId) {
        return invoiceRepository.findByOrder_Id(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Invoice not found for order: " + orderId));
    }

    /**
     * Get invoice file as resource for download
     */
    @Transactional
    public Resource getInvoiceFile(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
            .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceId));

        if (!invoice.hasFile()) {
            throw new BadRequestException("Invoice file not available");
        }

        try {
            Path file = Paths.get(invoice.getFilePath());
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() && resource.isReadable()) {
                // Update download count
                invoice.incrementDownloadCount();
                invoiceRepository.save(invoice);
                
                return resource;
            } else {
                throw new ResourceNotFoundException("Invoice file not found or not readable");
            }
        } catch (Exception e) {
            log.error("Error accessing invoice file {}: {}", invoice.getFilePath(), e.getMessage(), e);
            throw new BadRequestException("Failed to access invoice file");
        }
    }

    /**
     * Send invoice via email
     */
    @Transactional
    public void sendInvoice(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
            .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceId));

        if (!invoice.isGenerated()) {
            throw new BadRequestException("Invoice is not generated yet");
        }

        try {
            // Send email notification with invoice attachment
            notificationService.sendInvoiceEmail(
                invoice.getOrder().getCustomerEmail(),
                invoice.getOrder().getBillingFullName(),
                invoice,
                invoice.getFilePath()
            );

            // Mark as sent
            invoice.markAsSent();
            invoiceRepository.save(invoice);

            log.info("Invoice {} sent to {}", invoice.getInvoiceNumber(), invoice.getOrder().getCustomerEmail());

        } catch (Exception e) {
            log.error("Error sending invoice {}: {}", invoice.getInvoiceNumber(), e.getMessage(), e);
            throw new BadRequestException("Failed to send invoice: " + e.getMessage());
        }
    }

    /**
     * Regenerate existing invoice
     */
    @Transactional
    public Invoice regenerateInvoice(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
            .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceId));

        try {
            // Delete old file if exists
            if (invoice.hasFile()) {
                Path oldFile = Paths.get(invoice.getFilePath());
                Files.deleteIfExists(oldFile);
            }

            // Generate new PDF
            String fileName = invoice.getInvoiceNumber() + ".pdf";
            String filePath = generatePDFInvoice(invoice.getOrder(), invoice, fileName);
            
            // Update invoice
            Path file = Paths.get(filePath);
            invoice.markAsGenerated(filePath, fileName, Files.size(file));
            invoice = invoiceRepository.save(invoice);

            log.info("Successfully regenerated invoice {}", invoice.getInvoiceNumber());
            return invoice;

        } catch (Exception e) {
            log.error("Error regenerating invoice {}: {}", invoice.getInvoiceNumber(), e.getMessage(), e);
            throw new BadRequestException("Failed to regenerate invoice: " + e.getMessage());
        }
    }

    /**
     * Delete invoice
     */
    @Transactional
    public void deleteInvoice(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
            .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceId));

        try {
            // Delete file if exists
            if (invoice.hasFile()) {
                Path file = Paths.get(invoice.getFilePath());
                Files.deleteIfExists(file);
            }

            // Delete database record
            invoiceRepository.delete(invoice);

            log.info("Deleted invoice {}", invoice.getInvoiceNumber());

        } catch (Exception e) {
            log.error("Error deleting invoice {}: {}", invoice.getInvoiceNumber(), e.getMessage(), e);
            throw new BadRequestException("Failed to delete invoice: " + e.getMessage());
        }
    }

    // Private helper methods

    private String generateInvoiceNumber(Order order) {
        String prefix = "INV-" + order.getOrderNumber() + "-";
        String timestamp = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        // Check for existing invoice numbers with same pattern
        String pattern = prefix + timestamp + "%";
        List<String> existingNumbers = invoiceRepository.findInvoiceNumbersLike(
            pattern, 
            org.springframework.data.domain.PageRequest.of(0, 1)
        );
        
        if (existingNumbers.isEmpty()) {
            return prefix + timestamp + "-001";
        } else {
            // Extract sequence number and increment
            String lastNumber = existingNumbers.get(0);
            String[] parts = lastNumber.split("-");
            int sequence = Integer.parseInt(parts[parts.length - 1]) + 1;
            return prefix + timestamp + "-" + String.format("%03d", sequence);
        }
    }

    private String generatePDFInvoice(Order order, Invoice invoice, String fileName) throws IOException {
        // Create storage directory if it doesn't exist
        Path storageDir = Paths.get(invoiceStoragePath);
        Files.createDirectories(storageDir);

        // Generate file path
        Path filePath = storageDir.resolve(fileName);

        // Generate PDF content using a simple approach
        // In a real implementation, you would use a library like iText, Apache PDFBox, or similar
        String pdfContent = generateInvoiceContent(order, invoice);
        
        // For now, we'll create a simple text file as placeholder
        // TODO: Implement actual PDF generation with proper library
        Files.write(filePath, pdfContent.getBytes());

        log.info("Generated invoice PDF at: {}", filePath.toString());
        return filePath.toString();
    }

    private String generateInvoiceContent(Order order, Invoice invoice) {
        StringBuilder content = new StringBuilder();
        
        // Invoice header
        content.append("INVOICE\n");
        content.append("Invoice Number: ").append(invoice.getInvoiceNumber()).append("\n");
        content.append("Date: ").append(ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))).append("\n\n");
        
        // Order information
        content.append("Order Number: ").append(order.getOrderNumber()).append("\n");
        content.append("Order Date: ").append(order.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))).append("\n\n");
        
        // Customer information
        content.append("BILL TO:\n");
        content.append(order.getBillingFullName()).append("\n");
        content.append(order.getBillingAddressLine1()).append("\n");
        content.append(order.getBillingCity()).append(", ").append(order.getBillingPostalCode()).append("\n");
        content.append(order.getBillingCountry()).append("\n");
        content.append("Email: ").append(order.getCustomerEmail()).append("\n\n");
        
        // Shipping information if different
        if (!order.getBillingAddressLine1().equals(order.getShippingAddressLine1())) {
            content.append("SHIP TO:\n");
            content.append(order.getShippingFullName()).append("\n");
            content.append(order.getShippingAddressLine1()).append("\n");
            content.append(order.getShippingCity()).append(", ").append(order.getShippingPostalCode()).append("\n");
            content.append(order.getShippingCountry()).append("\n\n");
        }
        
        // Order items
        content.append("ITEMS:\n");
        content.append("Description\t\tQuantity\tPrice\tTotal\n");
        content.append("-----------------------------------------------------\n");
        
        for (OrderItem item : order.getOrderItems()) {
            content.append(item.getProductName()).append("\t\t");
            content.append(item.getQuantity()).append("\t\t");
            content.append(item.getUnitPrice()).append("\t");
            content.append(item.getTotalPrice()).append("\n");
        }
        
        content.append("-----------------------------------------------------\n");
        
        // Totals
        content.append("Subtotal: ").append(order.getSubtotal()).append(" ").append(order.getCurrency()).append("\n");
        
        if (order.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            content.append("Discount: -").append(order.getDiscountAmount()).append(" ").append(order.getCurrency()).append("\n");
        }
        
        if (order.getTaxAmount().compareTo(BigDecimal.ZERO) > 0) {
            content.append("Tax: ").append(order.getTaxAmount()).append(" ").append(order.getCurrency()).append("\n");
        }
        
        if (order.getShippingAmount().compareTo(BigDecimal.ZERO) > 0) {
            content.append("Shipping: ").append(order.getShippingAmount()).append(" ").append(order.getCurrency()).append("\n");
        }
        
        content.append("TOTAL: ").append(order.getTotalAmount()).append(" ").append(order.getCurrency()).append("\n\n");
        
        // Applied coupons
        if (order.getAppliedCoupons() != null && !order.getAppliedCoupons().isEmpty()) {
            content.append("APPLIED COUPONS:\n");
            for (OrderCoupon coupon : order.getAppliedCoupons()) {
                content.append("- ").append(coupon.getCouponCode()).append(" (").append(coupon.getCouponName()).append("): -");
                content.append(coupon.getDiscountAmount()).append(" ").append(coupon.getCurrency()).append("\n");
            }
            content.append("\n");
        }
        
        // Footer
        content.append("Thank you for your business!\n");
        content.append("Ocean Shopping Center\n");
        content.append("support@oceanshopping.com\n");
        
        return content.toString();
    }
}