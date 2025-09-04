package com.ocean.shopping.repository;

import com.ocean.shopping.model.entity.Notification;
import com.ocean.shopping.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Notification entity
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /**
     * Find notifications for a user ordered by creation time (newest first)
     */
    @Query("SELECT n FROM Notification n WHERE n.targetUser = :targetUser ORDER BY n.createdAt DESC")
    Page<Notification> findByTargetUserOrderByCreatedAtDesc(@Param("targetUser") User targetUser, Pageable pageable);

    /**
     * Find unread notifications for a user
     */
    @Query("SELECT n FROM Notification n WHERE n.targetUser = :targetUser AND n.isRead = false ORDER BY n.createdAt DESC")
    List<Notification> findUnreadByTargetUser(@Param("targetUser") User targetUser);

    /**
     * Count unread notifications for a user
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.targetUser = :targetUser AND n.isRead = false")
    long countUnreadByTargetUser(@Param("targetUser") User targetUser);

    /**
     * Find notifications by type for a user
     */
    @Query("SELECT n FROM Notification n WHERE n.targetUser = :targetUser AND n.type = :type ORDER BY n.createdAt DESC")
    Page<Notification> findByTargetUserAndType(@Param("targetUser") User targetUser, @Param("type") Notification.NotificationType type, Pageable pageable);

    /**
     * Find notifications by priority for a user
     */
    @Query("SELECT n FROM Notification n WHERE n.targetUser = :targetUser AND n.priority = :priority ORDER BY n.createdAt DESC")
    Page<Notification> findByTargetUserAndPriority(@Param("targetUser") User targetUser, @Param("priority") Notification.Priority priority, Pageable pageable);

    /**
     * Find high priority notifications for a user
     */
    @Query("SELECT n FROM Notification n WHERE n.targetUser = :targetUser AND n.priority IN ('HIGH', 'URGENT') ORDER BY n.createdAt DESC")
    List<Notification> findHighPriorityByTargetUser(@Param("targetUser") User targetUser);

    /**
     * Find urgent notifications for a user
     */
    @Query("SELECT n FROM Notification n WHERE n.targetUser = :targetUser AND n.priority = 'URGENT' ORDER BY n.createdAt DESC")
    List<Notification> findUrgentByTargetUser(@Param("targetUser") User targetUser);

    /**
     * Find pending notifications (not delivered yet)
     */
    @Query("SELECT n FROM Notification n WHERE n.deliveryStatus = 'PENDING' ORDER BY n.createdAt ASC")
    List<Notification> findPendingNotifications();

    /**
     * Find failed notifications for retry
     */
    @Query("SELECT n FROM Notification n WHERE n.deliveryStatus = 'FAILED' AND n.createdAt > :since ORDER BY n.createdAt ASC")
    List<Notification> findFailedNotificationsSince(@Param("since") ZonedDateTime since);

    /**
     * Find expired notifications
     */
    @Query("SELECT n FROM Notification n WHERE n.expiresAt IS NOT NULL AND n.expiresAt < :now")
    List<Notification> findExpiredNotifications(@Param("now") ZonedDateTime now);

    /**
     * Find recent notifications for a user
     */
    @Query("SELECT n FROM Notification n WHERE n.targetUser = :targetUser AND n.createdAt > :since ORDER BY n.createdAt DESC")
    List<Notification> findRecentByTargetUser(@Param("targetUser") User targetUser, @Param("since") ZonedDateTime since);

    /**
     * Find notifications by multiple types
     */
    @Query("SELECT n FROM Notification n WHERE n.targetUser = :targetUser AND n.type IN :types ORDER BY n.createdAt DESC")
    Page<Notification> findByTargetUserAndTypeIn(@Param("targetUser") User targetUser, @Param("types") List<Notification.NotificationType> types, Pageable pageable);

    /**
     * Find notifications by date range
     */
    @Query("SELECT n FROM Notification n WHERE n.targetUser = :targetUser AND n.createdAt BETWEEN :startDate AND :endDate ORDER BY n.createdAt DESC")
    List<Notification> findByTargetUserAndDateRange(@Param("targetUser") User targetUser, @Param("startDate") ZonedDateTime startDate, @Param("endDate") ZonedDateTime endDate);

    /**
     * Count notifications by type for a user
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.targetUser = :targetUser AND n.type = :type")
    long countByTargetUserAndType(@Param("targetUser") User targetUser, @Param("type") Notification.NotificationType type);

    /**
     * Find notifications with action URL
     */
    @Query("SELECT n FROM Notification n WHERE n.targetUser = :targetUser AND n.actionUrl IS NOT NULL ORDER BY n.createdAt DESC")
    List<Notification> findWithActionUrlByTargetUser(@Param("targetUser") User targetUser);

    /**
     * Mark multiple notifications as read
     */
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt WHERE n.id IN :ids")
    void markAsRead(@Param("ids") List<UUID> ids, @Param("readAt") ZonedDateTime readAt);

    /**
     * Mark multiple notifications as delivered
     */
    @Query("UPDATE Notification n SET n.deliveryStatus = 'DELIVERED', n.deliveredAt = :deliveredAt WHERE n.id IN :ids")
    void markAsDelivered(@Param("ids") List<UUID> ids, @Param("deliveredAt") ZonedDateTime deliveredAt);

    /**
     * Delete expired notifications
     */
    @Query("DELETE FROM Notification n WHERE n.expiresAt IS NOT NULL AND n.expiresAt < :now")
    void deleteExpiredNotifications(@Param("now") ZonedDateTime now);
}