package com.ocean.shopping.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for sending chat messages
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Chat message send request")
public class ChatMessageRequest {

    @Schema(description = "Message content", example = "Hello, I have a question about this product")
    @NotBlank(message = "Message content cannot be empty")
    @Size(max = 1000, message = "Message content cannot exceed 1000 characters")
    private String content;

    @Schema(description = "Receiver user ID", example = "123e4567-e89b-12d3-a456-426614174000")
    @NotNull(message = "Receiver ID is required")
    private UUID receiverId;

    @Schema(description = "Optional conversation ID for grouping messages", example = "123e4567-e89b-12d3-a456-426614174001")
    private UUID conversationId;

    @Schema(description = "Message type", example = "TEXT", allowableValues = {"TEXT", "IMAGE", "FILE", "SYSTEM"})
    @Builder.Default
    private MessageType messageType = MessageType.TEXT;

    @Schema(description = "Optional attachment URL", example = "https://example.com/file.pdf")
    @Size(max = 500, message = "Attachment URL cannot exceed 500 characters")
    private String attachmentUrl;

    @Schema(description = "Optional attachment file name", example = "document.pdf")
    @Size(max = 255, message = "Attachment name cannot exceed 255 characters")
    private String attachmentName;

    /**
     * Message types enum
     */
    public enum MessageType {
        TEXT, IMAGE, FILE, SYSTEM
    }
}