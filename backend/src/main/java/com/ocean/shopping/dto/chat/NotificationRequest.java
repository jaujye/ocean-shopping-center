package com.ocean.shopping.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Request DTO for creating notifications
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Notification creation request")
public class NotificationRequest {

    @Schema(description = "Notification title", example = "Order Update")
    @NotBlank(message = "Notification title cannot be empty")
    @Size(max = 200, message = "Notification title cannot exceed 200 characters")
    private String title;

    @Schema(description = "Notification message", example = "Your order has been shipped")
    @NotBlank(message = "Notification message cannot be empty")
    @Size(max = 1000, message = "Notification message cannot exceed 1000 characters")
    private String message;

    @Schema(description = "Notification type", example = "ORDER_UPDATE")
    @NotNull(message = "Notification type is required")
    private NotificationType type;

    @Schema(description = "Target user IDs for the notification")
    private List<UUID> targetUserIds;

    @Schema(description = "Priority level", example = "NORMAL")
    @Builder.Default
    private Priority priority = Priority.NORMAL;

    @Schema(description = "Additional data for the notification")
    private Map<String, Object> data;

    @Schema(description = "URL to navigate when notification is clicked", example = "/orders/123")
    @Size(max = 500, message = "Action URL cannot exceed 500 characters")
    private String actionUrl;

    @Schema(description = "Icon URL for the notification", example = "https://example.com/icon.png")
    @Size(max = 500, message = "Icon URL cannot exceed 500 characters")
    private String iconUrl;

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
}