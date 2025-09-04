package com.ocean.shopping.service;

import com.ocean.shopping.dto.chat.NotificationRequest;
import com.ocean.shopping.dto.chat.NotificationResponse;
import com.ocean.shopping.model.entity.Notification;
import com.ocean.shopping.model.entity.User;
import com.ocean.shopping.model.entity.enums.UserRole;
import com.ocean.shopping.model.entity.enums.UserStatus;
import com.ocean.shopping.repository.NotificationRepository;
import com.ocean.shopping.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.ZonedDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NotificationService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService Tests")
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificationService notificationService;

    private User targetUser;
    private NotificationRequest notificationRequest;

    @BeforeEach
    void setUp() {
        // Create test user
        targetUser = User.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .firstName("Test")
                .lastName("User")
                .role(UserRole.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();

        // Create test notification request
        notificationRequest = NotificationRequest.builder()
                .title("Order Update")
                .message("Your order has been shipped")
                .type(NotificationRequest.NotificationType.ORDER_UPDATE)
                .priority(NotificationRequest.Priority.NORMAL)
                .actionUrl("/orders/123")
                .iconUrl("https://example.com/icon.png")
                .data(Map.of("orderId", "123", "trackingNumber", "ABC123"))
                .build();
    }

    @Test
    @DisplayName("Should send notification to single user successfully")
    void shouldSendNotificationToSingleUserSuccessfully() {
        // Given
        when(userRepository.findById(targetUser.getId())).thenReturn(Optional.of(targetUser));

        Notification savedNotification = createTestNotification();
        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);

        // When
        NotificationResponse result = notificationService.sendNotification(targetUser.getId(), notificationRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo(notificationRequest.getTitle());
        assertThat(result.getMessage()).isEqualTo(notificationRequest.getMessage());
        assertThat(result.getType()).isEqualTo(notificationRequest.getType());
        assertThat(result.getTargetUser().getId()).isEqualTo(targetUser.getId());

        verify(notificationRepository, times(2)).save(any(Notification.class)); // Initial save + mark as delivered/failed
        verify(messagingTemplate).convertAndSendToUser(
                eq(targetUser.getId().toString()),
                eq("/queue/notifications"),
                any(NotificationResponse.class)
        );
    }

    @Test
    @DisplayName("Should send bulk notification successfully")
    void shouldSendBulkNotificationSuccessfully() {
        // Given
        User user1 = createTestUser("user1@example.com");
        User user2 = createTestUser("user2@example.com");
        List<UUID> targetUserIds = Arrays.asList(user1.getId(), user2.getId());
        
        notificationRequest.setTargetUserIds(targetUserIds);
        
        when(userRepository.findAllById(targetUserIds)).thenReturn(Arrays.asList(user1, user2));

        Notification savedNotification1 = createTestNotificationForUser(user1);
        Notification savedNotification2 = createTestNotificationForUser(user2);
        when(notificationRepository.save(any(Notification.class)))
                .thenReturn(savedNotification1, savedNotification1, savedNotification2, savedNotification2);

        // When
        List<NotificationResponse> result = notificationService.sendBulkNotification(notificationRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        
        verify(notificationRepository, times(4)).save(any(Notification.class)); // 2 users * 2 saves each
        verify(messagingTemplate, times(2)).convertAndSendToUser(
                anyString(),
                eq("/queue/notifications"),
                any(NotificationResponse.class)
        );
    }

    @Test
    @DisplayName("Should throw exception for empty target user IDs in bulk notification")
    void shouldThrowExceptionForEmptyTargetUserIds() {
        // Given
        notificationRequest.setTargetUserIds(Collections.emptyList());

        // When & Then
        assertThatThrownBy(() -> notificationService.sendBulkNotification(notificationRequest))
                .isInstanceOf(com.ocean.shopping.exception.BadRequestException.class)
                .hasMessageContaining("Target user IDs cannot be empty");
    }

    @Test
    @DisplayName("Should get user notifications successfully")
    void shouldGetUserNotificationsSuccessfully() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Notification> notifications = Arrays.asList(
                createTestNotification(),
                createTestNotification()
        );
        Page<Notification> notificationPage = new PageImpl<>(notifications, pageable, notifications.size());

        when(userRepository.findById(targetUser.getId())).thenReturn(Optional.of(targetUser));
        when(notificationRepository.findByTargetUserOrderByCreatedAtDesc(targetUser, pageable))
                .thenReturn(notificationPage);

        // When
        Page<NotificationResponse> result = notificationService.getUserNotifications(targetUser.getId(), pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);

        verify(notificationRepository).findByTargetUserOrderByCreatedAtDesc(targetUser, pageable);
    }

    @Test
    @DisplayName("Should get unread notifications successfully")
    void shouldGetUnreadNotificationsSuccessfully() {
        // Given
        List<Notification> unreadNotifications = Arrays.asList(
                createUnreadNotification(),
                createUnreadNotification()
        );

        when(userRepository.findById(targetUser.getId())).thenReturn(Optional.of(targetUser));
        when(notificationRepository.findUnreadByTargetUser(targetUser)).thenReturn(unreadNotifications);

        // When
        List<NotificationResponse> result = notificationService.getUnreadNotifications(targetUser.getId());

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        result.forEach(notification -> assertThat(notification.getIsRead()).isFalse());

        verify(notificationRepository).findUnreadByTargetUser(targetUser);
    }

    @Test
    @DisplayName("Should get unread notification count successfully")
    void shouldGetUnreadNotificationCountSuccessfully() {
        // Given
        when(userRepository.findById(targetUser.getId())).thenReturn(Optional.of(targetUser));
        when(notificationRepository.countUnreadByTargetUser(targetUser)).thenReturn(7L);

        // When
        long result = notificationService.getUnreadNotificationCount(targetUser.getId());

        // Then
        assertThat(result).isEqualTo(7L);
        verify(notificationRepository).countUnreadByTargetUser(targetUser);
    }

    @Test
    @DisplayName("Should mark notification as read successfully")
    void shouldMarkNotificationAsReadSuccessfully() {
        // Given
        Notification notification = createUnreadNotification();
        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));

        // When
        notificationService.markNotificationAsRead(targetUser.getId(), notification.getId());

        // Then
        assertThat(notification.getIsRead()).isTrue();
        assertThat(notification.getReadAt()).isNotNull();

        verify(notificationRepository).save(notification);
        verify(messagingTemplate).convertAndSendToUser(
                eq(targetUser.getId().toString()),
                eq("/queue/notifications"),
                any(Map.class)
        );
    }

    @Test
    @DisplayName("Should not mark already read notification")
    void shouldNotMarkAlreadyReadNotification() {
        // Given
        Notification notification = createTestNotification();
        notification.setIsRead(true);
        notification.setReadAt(ZonedDateTime.now());

        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));

        // When
        notificationService.markNotificationAsRead(targetUser.getId(), notification.getId());

        // Then
        verify(notificationRepository, never()).save(any(Notification.class));
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Should mark multiple notifications as read successfully")
    void shouldMarkMultipleNotificationsAsReadSuccessfully() {
        // Given
        List<UUID> notificationIds = Arrays.asList(UUID.randomUUID(), UUID.randomUUID());
        List<Notification> notifications = Arrays.asList(
                createUnreadNotificationWithId(notificationIds.get(0)),
                createUnreadNotificationWithId(notificationIds.get(1))
        );

        when(userRepository.findById(targetUser.getId())).thenReturn(Optional.of(targetUser));
        when(notificationRepository.findAllById(notificationIds)).thenReturn(notifications);

        // When
        notificationService.markNotificationsAsRead(targetUser.getId(), notificationIds);

        // Then
        verify(notificationRepository).saveAll(anyList());
        verify(messagingTemplate).convertAndSendToUser(
                eq(targetUser.getId().toString()),
                eq("/queue/notifications"),
                any(Map.class)
        );

        // Verify all notifications were marked as read
        notifications.forEach(notification -> {
            assertThat(notification.getIsRead()).isTrue();
            assertThat(notification.getReadAt()).isNotNull();
        });
    }

    @Test
    @DisplayName("Should throw exception when marking notifications not belonging to user")
    void shouldThrowExceptionWhenMarkingNotificationsBelongingToOtherUser() {
        // Given
        User otherUser = createTestUser("other@example.com");
        List<UUID> notificationIds = Arrays.asList(UUID.randomUUID());
        List<Notification> notifications = Arrays.asList(
                createNotificationForUser(otherUser, notificationIds.get(0))
        );

        when(userRepository.findById(targetUser.getId())).thenReturn(Optional.of(targetUser));
        when(notificationRepository.findAllById(notificationIds)).thenReturn(notifications);

        // When & Then
        assertThatThrownBy(() -> notificationService.markNotificationsAsRead(targetUser.getId(), notificationIds))
                .isInstanceOf(com.ocean.shopping.exception.BadRequestException.class)
                .hasMessageContaining("Cannot mark notifications that don't belong to the user");
    }

    @Test
    @DisplayName("Should delete notification successfully")
    void shouldDeleteNotificationSuccessfully() {
        // Given
        Notification notification = createTestNotification();
        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));

        // When
        notificationService.deleteNotification(targetUser.getId(), notification.getId());

        // Then
        verify(notificationRepository).delete(notification);
        verify(messagingTemplate).convertAndSendToUser(
                eq(targetUser.getId().toString()),
                eq("/queue/notifications"),
                any(Map.class)
        );
    }

    @Test
    @DisplayName("Should get notifications by type successfully")
    void shouldGetNotificationsByTypeSuccessfully() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Notification.NotificationType type = Notification.NotificationType.ORDER_UPDATE;
        List<Notification> notifications = Arrays.asList(createTestNotification());
        Page<Notification> notificationPage = new PageImpl<>(notifications, pageable, notifications.size());

        when(userRepository.findById(targetUser.getId())).thenReturn(Optional.of(targetUser));
        when(notificationRepository.findByTargetUserAndType(targetUser, type, pageable))
                .thenReturn(notificationPage);

        // When
        Page<NotificationResponse> result = notificationService.getNotificationsByType(targetUser.getId(), type, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(notificationRepository).findByTargetUserAndType(targetUser, type, pageable);
    }

    @Test
    @DisplayName("Should get high priority notifications successfully")
    void shouldGetHighPriorityNotificationsSuccessfully() {
        // Given
        List<Notification> highPriorityNotifications = Arrays.asList(
                createHighPriorityNotification(),
                createUrgentNotification()
        );

        when(userRepository.findById(targetUser.getId())).thenReturn(Optional.of(targetUser));
        when(notificationRepository.findHighPriorityByTargetUser(targetUser))
                .thenReturn(highPriorityNotifications);

        // When
        List<NotificationResponse> result = notificationService.getHighPriorityNotifications(targetUser.getId());

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        verify(notificationRepository).findHighPriorityByTargetUser(targetUser);
    }

    @Test
    @DisplayName("Should process pending notifications successfully")
    void shouldProcessPendingNotificationsSuccessfully() {
        // Given
        List<Notification> pendingNotifications = Arrays.asList(
                createPendingNotification(),
                createPendingNotification()
        );

        when(notificationRepository.findPendingNotifications()).thenReturn(pendingNotifications);

        // When
        notificationService.processPendingNotifications();

        // Then
        verify(notificationRepository).findPendingNotifications();
        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    @DisplayName("Should clean up expired notifications successfully")
    void shouldCleanupExpiredNotificationsSuccessfully() {
        // Given
        List<Notification> expiredNotifications = Arrays.asList(
                createExpiredNotification(),
                createExpiredNotification()
        );

        when(notificationRepository.findExpiredNotifications(any(ZonedDateTime.class)))
                .thenReturn(expiredNotifications);

        // When
        notificationService.cleanupExpiredNotifications();

        // Then
        verify(notificationRepository).findExpiredNotifications(any(ZonedDateTime.class));
        verify(notificationRepository).deleteExpiredNotifications(any(ZonedDateTime.class));
    }

    @Test
    @DisplayName("Should validate security - user can read own notification")
    void shouldValidateSecurityUserCanReadOwnNotification() {
        // Given
        Notification notification = createTestNotification();
        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));

        // When
        boolean result = notificationService.canReadNotification(targetUser.getId(), notification.getId());

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should validate security - user cannot read other's notification")
    void shouldValidateSecurityUserCannotReadOthersNotification() {
        // Given
        User otherUser = createTestUser("other@example.com");
        Notification notification = createNotificationForUser(otherUser, UUID.randomUUID());
        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));

        // When
        boolean result = notificationService.canReadNotification(targetUser.getId(), notification.getId());

        // Then
        assertThat(result).isFalse();
    }

    // Helper methods
    private User createTestUser(String email) {
        return User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .firstName("Test")
                .lastName("User")
                .role(UserRole.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();
    }

    private Notification createTestNotification() {
        return Notification.builder()
                .id(UUID.randomUUID())
                .title(notificationRequest.getTitle())
                .message(notificationRequest.getMessage())
                .type(Notification.NotificationType.ORDER_UPDATE)
                .priority(Notification.Priority.NORMAL)
                .targetUser(targetUser)
                .actionUrl(notificationRequest.getActionUrl())
                .iconUrl(notificationRequest.getIconUrl())
                .data(notificationRequest.getData())
                .deliveryStatus(Notification.DeliveryStatus.DELIVERED)
                .isRead(false)
                .createdAt(ZonedDateTime.now())
                .build();
    }

    private Notification createTestNotificationForUser(User user) {
        Notification notification = createTestNotification();
        notification.setTargetUser(user);
        return notification;
    }

    private Notification createUnreadNotification() {
        Notification notification = createTestNotification();
        notification.setIsRead(false);
        return notification;
    }

    private Notification createUnreadNotificationWithId(UUID id) {
        Notification notification = createUnreadNotification();
        notification.setId(id);
        return notification;
    }

    private Notification createNotificationForUser(User user, UUID id) {
        Notification notification = createTestNotification();
        notification.setId(id);
        notification.setTargetUser(user);
        return notification;
    }

    private Notification createHighPriorityNotification() {
        Notification notification = createTestNotification();
        notification.setPriority(Notification.Priority.HIGH);
        return notification;
    }

    private Notification createUrgentNotification() {
        Notification notification = createTestNotification();
        notification.setPriority(Notification.Priority.URGENT);
        return notification;
    }

    private Notification createPendingNotification() {
        Notification notification = createTestNotification();
        notification.setDeliveryStatus(Notification.DeliveryStatus.PENDING);
        return notification;
    }

    private Notification createExpiredNotification() {
        Notification notification = createTestNotification();
        notification.setExpiresAt(ZonedDateTime.now().minusDays(1));
        return notification;
    }
}