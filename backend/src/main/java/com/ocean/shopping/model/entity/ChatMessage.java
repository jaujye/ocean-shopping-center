package com.ocean.shopping.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.Map;
import java.util.UUID;

/**
 * Chat message entity for real-time communication
 */
@Entity
@Table(name = "chat_messages", indexes = {
    @Index(name = "idx_chat_messages_conversation_id", columnList = "conversation_id"),
    @Index(name = "idx_chat_messages_sender_id", columnList = "sender_id"),
    @Index(name = "idx_chat_messages_receiver_id", columnList = "receiver_id"),
    @Index(name = "idx_chat_messages_created_at", columnList = "created_at"),
    @Index(name = "idx_chat_messages_delivery_status", columnList = "delivery_status"),
    @Index(name = "idx_chat_messages_is_read", columnList = "is_read")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage extends BaseEntity {

    @Column(name = "conversation_id", nullable = false)
    @NotNull(message = "Conversation ID is required")
    private UUID conversationId;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "Message content cannot be empty")
    @Size(max = 1000, message = "Message content cannot exceed 1000 characters")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false)
    @Builder.Default
    private MessageType messageType = MessageType.TEXT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    @NotNull(message = "Sender is required")
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    @NotNull(message = "Receiver is required")
    private User receiver;

    @Column(name = "attachment_url")
    @Size(max = 500, message = "Attachment URL cannot exceed 500 characters")
    private String attachmentUrl;

    @Column(name = "attachment_name")
    @Size(max = 255, message = "Attachment name cannot exceed 255 characters")
    private String attachmentName;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false)
    @Builder.Default
    private DeliveryStatus deliveryStatus = DeliveryStatus.SENT;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "read_at")
    private java.time.ZonedDateTime readAt;

    @Column(name = "delivered_at")
    private java.time.ZonedDateTime deliveredAt;

    @ElementCollection
    @CollectionTable(
        name = "chat_message_metadata", 
        joinColumns = @JoinColumn(name = "message_id")
    )
    @MapKeyColumn(name = "metadata_key")
    @Column(name = "metadata_value")
    private Map<String, String> metadata;

    /**
     * Message types enum
     */
    public enum MessageType {
        TEXT, IMAGE, FILE, SYSTEM
    }

    /**
     * Delivery status enum
     */
    public enum DeliveryStatus {
        SENT, DELIVERED, READ, FAILED
    }

    /**
     * Mark message as read
     */
    public void markAsRead() {
        this.isRead = true;
        this.readAt = java.time.ZonedDateTime.now();
        if (this.deliveryStatus == DeliveryStatus.DELIVERED) {
            this.deliveryStatus = DeliveryStatus.READ;
        }
    }

    /**
     * Mark message as delivered
     */
    public void markAsDelivered() {
        this.deliveryStatus = DeliveryStatus.DELIVERED;
        this.deliveredAt = java.time.ZonedDateTime.now();
    }

    /**
     * Mark message as failed
     */
    public void markAsFailed() {
        this.deliveryStatus = DeliveryStatus.FAILED;
    }

    /**
     * Check if message has attachment
     */
    public boolean hasAttachment() {
        return attachmentUrl != null && !attachmentUrl.trim().isEmpty();
    }

    /**
     * Check if message is system generated
     */
    public boolean isSystemMessage() {
        return messageType == MessageType.SYSTEM;
    }
}