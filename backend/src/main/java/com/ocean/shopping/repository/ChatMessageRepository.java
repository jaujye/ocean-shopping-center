package com.ocean.shopping.repository;

import com.ocean.shopping.model.entity.ChatMessage;
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
 * Repository for ChatMessage entity
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    /**
     * Find messages in a conversation ordered by creation time
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.conversationId = :conversationId ORDER BY cm.createdAt ASC")
    Page<ChatMessage> findByConversationIdOrderByCreatedAtAsc(@Param("conversationId") UUID conversationId, Pageable pageable);

    /**
     * Find messages in a conversation after a specific timestamp
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.conversationId = :conversationId AND cm.createdAt > :since ORDER BY cm.createdAt ASC")
    List<ChatMessage> findByConversationIdAndCreatedAtAfter(@Param("conversationId") UUID conversationId, @Param("since") ZonedDateTime since);

    /**
     * Find messages between two users ordered by creation time
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE " +
           "(cm.sender = :user1 AND cm.receiver = :user2) OR " +
           "(cm.sender = :user2 AND cm.receiver = :user1) " +
           "ORDER BY cm.createdAt ASC")
    Page<ChatMessage> findMessagesBetweenUsers(@Param("user1") User user1, @Param("user2") User user2, Pageable pageable);

    /**
     * Find all conversations for a user (distinct conversation IDs)
     */
    @Query("SELECT DISTINCT cm.conversationId FROM ChatMessage cm WHERE cm.sender = :user OR cm.receiver = :user")
    List<UUID> findConversationsByUser(@Param("user") User user);

    /**
     * Find unread messages for a user
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.receiver = :receiver AND cm.isRead = false ORDER BY cm.createdAt DESC")
    List<ChatMessage> findUnreadMessagesByReceiver(@Param("receiver") User receiver);

    /**
     * Count unread messages for a user
     */
    @Query("SELECT COUNT(cm) FROM ChatMessage cm WHERE cm.receiver = :receiver AND cm.isRead = false")
    long countUnreadMessagesByReceiver(@Param("receiver") User receiver);

    /**
     * Count unread messages in a conversation for a user
     */
    @Query("SELECT COUNT(cm) FROM ChatMessage cm WHERE cm.conversationId = :conversationId AND cm.receiver = :receiver AND cm.isRead = false")
    long countUnreadMessagesInConversation(@Param("conversationId") UUID conversationId, @Param("receiver") User receiver);

    /**
     * Find the latest message in a conversation
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.conversationId = :conversationId ORDER BY cm.createdAt DESC")
    List<ChatMessage> findLatestMessageInConversation(@Param("conversationId") UUID conversationId, Pageable pageable);

    /**
     * Find messages with attachments in a conversation
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.conversationId = :conversationId AND cm.attachmentUrl IS NOT NULL ORDER BY cm.createdAt DESC")
    List<ChatMessage> findMessagesWithAttachments(@Param("conversationId") UUID conversationId);

    /**
     * Find messages by type in a conversation
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.conversationId = :conversationId AND cm.messageType = :messageType ORDER BY cm.createdAt DESC")
    List<ChatMessage> findByConversationIdAndMessageType(@Param("conversationId") UUID conversationId, @Param("messageType") ChatMessage.MessageType messageType);

    /**
     * Find failed messages for retry
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.deliveryStatus = 'FAILED' AND cm.createdAt > :since")
    List<ChatMessage> findFailedMessagesSince(@Param("since") ZonedDateTime since);

    /**
     * Find system messages in a conversation
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.conversationId = :conversationId AND cm.messageType = 'SYSTEM' ORDER BY cm.createdAt DESC")
    List<ChatMessage> findSystemMessages(@Param("conversationId") UUID conversationId);

    /**
     * Find messages sent by a user
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.sender = :sender ORDER BY cm.createdAt DESC")
    Page<ChatMessage> findBySender(@Param("sender") User sender, Pageable pageable);

    /**
     * Find messages received by a user
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.receiver = :receiver ORDER BY cm.createdAt DESC")
    Page<ChatMessage> findByReceiver(@Param("receiver") User receiver, Pageable pageable);

    /**
     * Find conversation participants
     */
    @Query("SELECT DISTINCT u FROM User u WHERE u IN " +
           "(SELECT cm.sender FROM ChatMessage cm WHERE cm.conversationId = :conversationId) OR u IN " +
           "(SELECT cm.receiver FROM ChatMessage cm WHERE cm.conversationId = :conversationId)")
    List<User> findConversationParticipants(@Param("conversationId") UUID conversationId);

    /**
     * Find messages in date range
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.conversationId = :conversationId AND cm.createdAt BETWEEN :startDate AND :endDate ORDER BY cm.createdAt ASC")
    List<ChatMessage> findMessagesByDateRange(@Param("conversationId") UUID conversationId, @Param("startDate") ZonedDateTime startDate, @Param("endDate") ZonedDateTime endDate);
}