package com.ocean.shopping.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.Map;

/**
 * Notification entity for real-time notifications
 */
@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notifications_target_user_id", columnList = "target_user_id"),
    @Index(name = "idx_notifications_type", columnList = "type"),
    @Index(name = "idx_notifications_priority", columnList = "priority"),
    @Index(name = "idx_notifications_is_read", columnList = "is_read"),
    @Index(name = "idx_notifications_delivery_status", columnList = "delivery_status"),
    @Index(name = "idx_notifications_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends BaseEntity {

    @Column(name = "title", nullable = false)
    @NotBlank(message = "Notification title cannot be empty")
    @Size(max = 200, message = "Notification title cannot exceed 200 characters")
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "Notification message cannot be empty")
    @Size(max = 1000, message = "Notification message cannot exceed 1000 characters")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    @NotNull(message = "Notification type is required")
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    @Builder.Default
    private Priority priority = Priority.NORMAL;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id", nullable = false)
    @NotNull(message = "Target user is required")
    private User targetUser;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "action_url")
    @Size(max = 500, message = "Action URL cannot exceed 500 characters")
    private String actionUrl;

    @Column(name = "icon_url")
    @Size(max = 500, message = "Icon URL cannot exceed 500 characters")
    private String iconUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false)
    @Builder.Default
    private DeliveryStatus deliveryStatus = DeliveryStatus.PENDING;

    @Column(name = "read_at")
    private ZonedDateTime readAt;

    @Column(name = "delivered_at")
    private ZonedDateTime deliveredAt;

    @Column(name = "expires_at")
    private ZonedDateTime expiresAt;

    @ElementCollection
    @CollectionTable(
        name = "notification_data", 
        joinColumns = @JoinColumn(name = "notification_id")
    )
    @MapKeyColumn(name = "data_key")
    @Column(name = "data_value", columnDefinition = "TEXT")
    private Map<String, Object> data;

    /**
     * Notification types enum
     */
    public enum NotificationType {
        ORDER_UPDATE,
        CHAT_MESSAGE,
        SYSTEM_ANNOUNCEMENT,
        PRODUCT_PROMOTION,
        STOCK_ALERT,
        PAYMENT_REMINDER,
        SHIPPING_UPDATE,
        REVIEW_REQUEST,
        SECURITY_ALERT
    }

    /**
     * Priority levels enum
     */
    public enum Priority {
        LOW, NORMAL, HIGH, URGENT
    }

    /**
     * Delivery status enum
     */
    public enum DeliveryStatus {
        PENDING, DELIVERED, FAILED
    }

    /**
     * Mark notification as read
     */
    public void markAsRead() {
        this.isRead = true;
        this.readAt = ZonedDateTime.now();
    }

    /**
     * Mark notification as delivered
     */
    public void markAsDelivered() {
        this.deliveryStatus = DeliveryStatus.DELIVERED;
        this.deliveredAt = ZonedDateTime.now();
    }

    /**
     * Mark notification as failed
     */
    public void markAsFailed() {
        this.deliveryStatus = DeliveryStatus.FAILED;
    }

    /**
     * Check if notification is expired
     */
    public boolean isExpired() {
        return expiresAt != null && ZonedDateTime.now().isAfter(expiresAt);
    }

    /**
     * Check if notification is urgent
     */
    public boolean isUrgent() {
        return priority == Priority.URGENT;
    }

    /**
     * Check if notification is high priority
     */
    public boolean isHighPriority() {
        return priority == Priority.HIGH || priority == Priority.URGENT;
    }
}