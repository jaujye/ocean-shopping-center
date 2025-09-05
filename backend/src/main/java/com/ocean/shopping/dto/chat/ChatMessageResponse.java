package com.ocean.shopping.dto.chat;

import com.ocean.shopping.dto.user.UserResponse;
import com.ocean.shopping.model.entity.ChatMessage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Response DTO for chat messages
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Chat message response")
public class ChatMessageResponse {

    @Schema(description = "Message unique identifier", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Schema(description = "Conversation unique identifier", example = "123e4567-e89b-12d3-a456-426614174001")
    private UUID conversationId;

    @Schema(description = "Message content", example = "Hello, I have a question about this product")
    private String content;

    @Schema(description = "Message type", example = "TEXT")
    private ChatMessageRequest.MessageType messageType;

    @Schema(description = "Message sender information")
    private UserResponse sender;

    @Schema(description = "Message receiver information")
    private UserResponse receiver;

    @Schema(description = "Attachment URL if any", example = "https://example.com/file.pdf")
    private String attachmentUrl;

    @Schema(description = "Attachment file name if any", example = "document.pdf")
    private String attachmentName;

    @Schema(description = "Message delivery status", example = "DELIVERED")
    private DeliveryStatus deliveryStatus;

    @Schema(description = "Message read status", example = "true")
    private Boolean isRead;

    @Schema(description = "Message timestamp", example = "2023-12-01T10:30:00Z")
    private ZonedDateTime timestamp;

    @Schema(description = "Message creation timestamp", example = "2023-12-01T10:30:00Z")
    private ZonedDateTime createdAt;

    /**
     * Delivery status enum
     */
    public enum DeliveryStatus {
        SENT, DELIVERED, READ, FAILED
    }

    /**
     * Create ChatMessageResponse from ChatMessage entity
     */
    public static ChatMessageResponse fromEntity(ChatMessage message) {
        return ChatMessageResponse.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .content(message.getContent())
                .messageType(ChatMessageRequest.MessageType.valueOf(message.getMessageType().name()))
                .sender(UserResponse.fromEntity(message.getSender()))
                .receiver(UserResponse.fromEntity(message.getReceiver()))
                .attachmentUrl(message.getAttachmentUrl())
                .attachmentName(message.getAttachmentName())
                .deliveryStatus(DeliveryStatus.valueOf(message.getDeliveryStatus().name()))
                .isRead(message.getIsRead())
                .timestamp(message.getCreatedAt()) // Use createdAt as timestamp
                .createdAt(message.getCreatedAt())
                .build();
    }
}