package com.ocean.shopping.dto.chat;

import com.ocean.shopping.dto.user.UserResponse;
import com.ocean.shopping.model.entity.Notification;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for notifications
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Notification response")
public class NotificationResponse {

    @Schema(description = "Notification unique identifier", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Schema(description = "Notification title", example = "Order Update")
    private String title;

    @Schema(description = "Notification message", example = "Your order has been shipped")
    private String message;

    @Schema(description = "Notification type", example = "ORDER_UPDATE")
    private NotificationRequest.NotificationType type;

    @Schema(description = "Priority level", example = "NORMAL")
    private NotificationRequest.Priority priority;

    @Schema(description = "Target user information")
    private UserResponse targetUser;

    @Schema(description = "Notification read status", example = "false")
    private Boolean isRead;

    @Schema(description = "Additional data for the notification")
    private Map<String, Object> data;

    @Schema(description = "URL to navigate when notification is clicked", example = "/orders/123")
    private String actionUrl;

    @Schema(description = "Icon URL for the notification", example = "https://example.com/icon.png")
    private String iconUrl;

    @Schema(description = "Delivery status", example = "DELIVERED")
    private DeliveryStatus deliveryStatus;

    @Schema(description = "Notification creation timestamp", example = "2023-12-01T10:30:00Z")
    private ZonedDateTime createdAt;

    @Schema(description = "Notification read timestamp", example = "2023-12-01T10:35:00Z")
    private ZonedDateTime readAt;

    @Schema(description = "Notification delivery timestamp", example = "2023-12-01T10:30:05Z")
    private ZonedDateTime deliveredAt;

    /**
     * Delivery status enum
     */
    public enum DeliveryStatus {
        PENDING, DELIVERED, FAILED
    }

    /**
     * Create NotificationResponse from Notification entity
     */
    public static NotificationResponse fromEntity(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(NotificationRequest.NotificationType.valueOf(notification.getType().name()))
                .priority(NotificationRequest.Priority.valueOf(notification.getPriority().name()))
                .targetUser(UserResponse.fromEntity(notification.getTargetUser()))
                .isRead(notification.getIsRead())
                .data(notification.getData())
                .actionUrl(notification.getActionUrl())
                .iconUrl(notification.getIconUrl())
                .deliveryStatus(DeliveryStatus.valueOf(notification.getDeliveryStatus().name()))
                .createdAt(notification.getCreatedAt())
                .readAt(notification.getReadAt())
                .deliveredAt(notification.getDeliveredAt())
                .build();
    }
}