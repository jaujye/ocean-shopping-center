package com.ocean.shopping.service;

import com.ocean.shopping.dto.chat.NotificationRequest;
import com.ocean.shopping.dto.chat.NotificationResponse;
import com.ocean.shopping.dto.chat.NotificationPreferencesRequest;
import com.ocean.shopping.exception.BadRequestException;
import com.ocean.shopping.exception.ResourceNotFoundException;
import com.ocean.shopping.model.entity.Notification;
import com.ocean.shopping.model.entity.User;
import com.ocean.shopping.model.entity.Order;
import com.ocean.shopping.model.entity.enums.OrderStatus;
import com.ocean.shopping.repository.NotificationRepository;
import com.ocean.shopping.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Service for managing notifications and real-time delivery
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Send notification to a single user
     */
    @Transactional
    public NotificationResponse sendNotification(UUID userId, NotificationRequest request) {
        log.info("Sending notification '{}' to user {}", request.getTitle(), userId);

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        return createAndSendNotification(targetUser, request);
    }

    /**
     * Send notification to multiple users
     */
    @Transactional
    public List<NotificationResponse> sendBulkNotification(NotificationRequest request) {
        log.info("Sending bulk notification '{}' to {} users", request.getTitle(), request.getTargetUserIds().size());

        if (request.getTargetUserIds() == null || request.getTargetUserIds().isEmpty()) {
            throw new BadRequestException("Target user IDs cannot be empty");
        }

        List<User> targetUsers = userRepository.findAllById(request.getTargetUserIds());
        
        if (targetUsers.size() != request.getTargetUserIds().size()) {
            log.warn("Some target users not found. Expected: {}, Found: {}", 
                    request.getTargetUserIds().size(), targetUsers.size());
        }

        return targetUsers.stream()
                .map(user -> createAndSendNotification(user, request))
                .collect(Collectors.toList());
    }

    /**
     * Send system announcement to all users
     */
    @Transactional
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public void sendSystemAnnouncement(NotificationRequest request) {
        log.info("Sending system announcement: {}", request.getTitle());

        // Override type to system announcement
        request.setType(NotificationRequest.NotificationType.SYSTEM_ANNOUNCEMENT);
        
        List<User> allUsers = userRepository.findAll();
        
        allUsers.parallelStream()
                .forEach(user -> createAndSendNotification(user, request));
        
        log.info("System announcement sent to {} users", allUsers.size());
    }

    /**
     * Get notifications for a user
     */
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUserNotifications(UUID userId, Pageable pageable) {
        log.debug("Getting notifications for user {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Page<Notification> notifications = notificationRepository
                .findByTargetUserOrderByCreatedAtDesc(user, pageable);

        return notifications.map(NotificationResponse::fromEntity);
    }

    /**
     * Get unread notifications for a user
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse> getUnreadNotifications(UUID userId) {
        log.debug("Getting unread notifications for user {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        List<Notification> notifications = notificationRepository.findUnreadByTargetUser(user);
        return notifications.stream()
                .map(NotificationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get unread notification count for a user
     */
    @Transactional(readOnly = true)
    public long getUnreadNotificationCount(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        return notificationRepository.countUnreadByTargetUser(user);
    }

    /**
     * Mark notification as read
     */
    @Transactional
    @PreAuthorize("@notificationService.canReadNotification(#userId, #notificationId)")
    public void markNotificationAsRead(UUID userId, UUID notificationId) {
        log.debug("Marking notification {} as read by user {}", notificationId, userId);

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));

        if (!notification.getIsRead()) {
            notification.markAsRead();
            notificationRepository.save(notification);
            
            // Send real-time update
            sendNotificationUpdate(userId, "NOTIFICATION_READ", Map.of("notificationId", notificationId));
        }
    }

    /**
     * Mark multiple notifications as read
     */
    @Transactional
    public void markNotificationsAsRead(UUID userId, List<UUID> notificationIds) {
        log.debug("Marking {} notifications as read by user {}", notificationIds.size(), userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Verify user owns all notifications
        List<Notification> notifications = notificationRepository.findAllById(notificationIds);
        
        boolean allBelongToUser = notifications.stream()
                .allMatch(notification -> notification.getTargetUser().getId().equals(userId));
        
        if (!allBelongToUser) {
            throw new BadRequestException("Cannot mark notifications that don't belong to the user");
        }

        ZonedDateTime now = ZonedDateTime.now();
        notifications.stream()
                .filter(notification -> !notification.getIsRead())
                .forEach(notification -> {
                    notification.setIsRead(true);
                    notification.setReadAt(now);
                });

        notificationRepository.saveAll(notifications);
        
        // Send real-time update
        sendNotificationUpdate(userId, "NOTIFICATIONS_READ", 
            Map.of("notificationIds", notificationIds, "count", notifications.size()));
    }

    /**
     * Delete notification
     */
    @Transactional
    @PreAuthorize("@notificationService.canDeleteNotification(#userId, #notificationId)")
    public void deleteNotification(UUID userId, UUID notificationId) {
        log.info("Deleting notification {} by user {}", notificationId, userId);

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));

        notificationRepository.delete(notification);
        
        // Send real-time update
        sendNotificationUpdate(userId, "NOTIFICATION_DELETED", Map.of("notificationId", notificationId));
    }

    /**
     * Get notifications by type
     */
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotificationsByType(UUID userId, 
                                                           Notification.NotificationType type, 
                                                           Pageable pageable) {
        log.debug("Getting notifications of type {} for user {}", type, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Page<Notification> notifications = notificationRepository
                .findByTargetUserAndType(user, type, pageable);

        return notifications.map(NotificationResponse::fromEntity);
    }

    /**
     * Get high priority notifications
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse> getHighPriorityNotifications(UUID userId) {
        log.debug("Getting high priority notifications for user {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        List<Notification> notifications = notificationRepository.findHighPriorityByTargetUser(user);
        return notifications.stream()
                .map(NotificationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Process pending notifications (for batch delivery)
     */
    @Async
    @Transactional
    public CompletableFuture<Void> processPendingNotifications() {
        log.debug("Processing pending notifications");

        List<Notification> pendingNotifications = notificationRepository.findPendingNotifications();
        
        pendingNotifications.parallelStream()
                .forEach(notification -> {
                    try {
                        deliverNotification(notification);
                        notification.markAsDelivered();
                        notificationRepository.save(notification);
                    } catch (Exception e) {
                        log.error("Failed to deliver notification {}: {}", notification.getId(), e.getMessage());
                        notification.markAsFailed();
                        notificationRepository.save(notification);
                    }
                });

        log.info("Processed {} pending notifications", pendingNotifications.size());
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Clean up expired notifications
     */
    @Transactional
    public void cleanupExpiredNotifications() {
        log.debug("Cleaning up expired notifications");

        List<Notification> expiredNotifications = notificationRepository
                .findExpiredNotifications(ZonedDateTime.now());

        if (!expiredNotifications.isEmpty()) {
            notificationRepository.deleteExpiredNotifications(ZonedDateTime.now());
            log.info("Deleted {} expired notifications", expiredNotifications.size());
        }
    }

    /**
     * Update notification preferences (placeholder - could be extended)
     */
    @Transactional
    public void updateNotificationPreferences(UUID userId, NotificationPreferencesRequest preferences) {
        log.info("Updating notification preferences for user {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // This is a placeholder - in a real implementation, you'd have a separate
        // NotificationPreferences entity and repository
        log.info("Notification preferences updated for user {}", user.getEmail());
    }

    /**
     * Retry failed notifications
     */
    @Async
    @Transactional
    public CompletableFuture<Void> retryFailedNotifications(int hoursBack) {
        log.info("Retrying failed notifications from last {} hours", hoursBack);

        ZonedDateTime since = ZonedDateTime.now().minusHours(hoursBack);
        List<Notification> failedNotifications = notificationRepository
                .findFailedNotificationsSince(since);

        failedNotifications.parallelStream()
                .forEach(notification -> {
                    try {
                        deliverNotification(notification);
                        notification.markAsDelivered();
                        notificationRepository.save(notification);
                        log.debug("Successfully retried notification {}", notification.getId());
                    } catch (Exception e) {
                        log.error("Failed to retry notification {}: {}", notification.getId(), e.getMessage());
                    }
                });

        log.info("Attempted retry for {} failed notifications", failedNotifications.size());
        return CompletableFuture.completedFuture(null);
    }

    // Security helper methods
    public boolean canReadNotification(UUID userId, UUID notificationId) {
        return notificationRepository.findById(notificationId)
                .map(notification -> notification.getTargetUser().getId().equals(userId))
                .orElse(false);
    }

    public boolean canDeleteNotification(UUID userId, UUID notificationId) {
        return canReadNotification(userId, notificationId);
    }

    // Private helper methods
    private NotificationResponse createAndSendNotification(User targetUser, NotificationRequest request) {
        Notification notification = Notification.builder()
                .title(request.getTitle())
                .message(request.getMessage())
                .type(Notification.NotificationType.valueOf(request.getType().name()))
                .priority(Notification.Priority.valueOf(request.getPriority().name()))
                .targetUser(targetUser)
                .actionUrl(request.getActionUrl())
                .iconUrl(request.getIconUrl())
                .data(request.getData())
                .deliveryStatus(Notification.DeliveryStatus.PENDING)
                .isRead(false)
                .build();

        Notification savedNotification = notificationRepository.save(notification);

        // Attempt immediate delivery
        try {
            deliverNotification(savedNotification);
            savedNotification.markAsDelivered();
            notificationRepository.save(savedNotification);
        } catch (Exception e) {
            log.error("Failed to deliver notification {}: {}", savedNotification.getId(), e.getMessage());
            savedNotification.markAsFailed();
            notificationRepository.save(savedNotification);
        }

        return NotificationResponse.fromEntity(savedNotification);
    }

    private void deliverNotification(Notification notification) {
        log.debug("Delivering notification {} to user {}", 
                notification.getId(), notification.getTargetUser().getId());

        NotificationResponse response = NotificationResponse.fromEntity(notification);
        
        // Send via WebSocket
        sendRealTimeNotification(notification.getTargetUser().getId(), response);

        // Here you could add other delivery methods:
        // - Email notifications
        // - Push notifications
        // - SMS notifications
        // Based on user preferences
    }

    private void sendRealTimeNotification(UUID userId, NotificationResponse notification) {
        try {
            messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/notifications",
                notification
            );
            log.debug("Real-time notification sent to user {}", userId);
        } catch (Exception e) {
            log.error("Failed to send real-time notification to user {}: {}", userId, e.getMessage());
            throw e;
        }
    }

    private void sendNotificationUpdate(UUID userId, String updateType, Map<String, Object> data) {
        try {
            Map<String, Object> update = new HashMap<>(data);
            update.put("type", updateType);
            update.put("timestamp", ZonedDateTime.now());
            
            messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/notifications",
                update
            );
            log.debug("Notification update '{}' sent to user {}", updateType, userId);
        } catch (Exception e) {
            log.error("Failed to send notification update to user {}: {}", userId, e.getMessage());
        }
    }

    // Order-specific email notifications

    /**
     * Send order status update email notification
     */
    @Async
    public void sendOrderStatusUpdateEmail(String customerEmail, Order order, OrderStatus oldStatus, OrderStatus newStatus) {
        try {
            log.info("Sending order status update email for order {} from {} to {}", 
                    order.getOrderNumber(), oldStatus, newStatus);

            String subject = String.format("Order %s - Status Update", order.getOrderNumber());
            String message = String.format("Your order %s has been updated from %s to %s", 
                    order.getOrderNumber(), getStatusDisplayText(oldStatus), getStatusDisplayText(newStatus));

            // Create in-app notification
            NotificationRequest notificationRequest = new NotificationRequest();
            notificationRequest.setTitle(subject);
            notificationRequest.setMessage(message);
            notificationRequest.setType(NotificationRequest.NotificationType.ORDER_UPDATE);
            notificationRequest.setPriority(NotificationRequest.Priority.MEDIUM);
            
            // Find user by email and send notification
            userRepository.findByEmail(customerEmail).ifPresent(user -> {
                createAndSendNotification(user, notificationRequest);
            });

            // TODO: Integrate with email service when available
            log.info("Order status update notification processed for {}", customerEmail);

        } catch (Exception e) {
            log.error("Failed to send order status update email to {}: {}", customerEmail, e.getMessage());
        }
    }

    /**
     * Send refund notification email
     */
    @Async
    public void sendRefundNotificationEmail(String customerEmail, Order order, BigDecimal amount, String reason) {
        try {
            log.info("Sending refund notification email for order {} - amount: {}", 
                    order.getOrderNumber(), amount);

            String subject = String.format("Refund Processed - Order %s", order.getOrderNumber());
            String message = String.format("A refund of %s %s has been processed for your order %s. Reason: %s", 
                    amount, order.getCurrency(), order.getOrderNumber(), reason);

            // Create in-app notification
            NotificationRequest notificationRequest = new NotificationRequest();
            notificationRequest.setTitle(subject);
            notificationRequest.setMessage(message);
            notificationRequest.setType(NotificationRequest.NotificationType.PAYMENT_UPDATE);
            notificationRequest.setPriority(NotificationRequest.Priority.HIGH);
            
            // Find user by email and send notification
            userRepository.findByEmail(customerEmail).ifPresent(user -> {
                createAndSendNotification(user, notificationRequest);
            });

            // TODO: Integrate with email service when available
            log.info("Refund notification processed for {}", customerEmail);

        } catch (Exception e) {
            log.error("Failed to send refund notification email to {}: {}", customerEmail, e.getMessage());
        }
    }

    private String getStatusDisplayText(OrderStatus status) {
        return switch (status) {
            case PENDING -> "Awaiting Payment";
            case CONFIRMED -> "Order Confirmed";
            case PROCESSING -> "Being Prepared";
            case SHIPPED -> "Shipped";
            case DELIVERED -> "Delivered";
            case CANCELLED -> "Cancelled";
            case RETURNED -> "Returned";
        };
    }
}