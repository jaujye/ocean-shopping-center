package com.ocean.shopping.dto.chat;

import com.ocean.shopping.dto.user.UserResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for chat conversations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Chat conversation response")
public class ConversationResponse {

    @Schema(description = "Conversation unique identifier", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID conversationId;

    @Schema(description = "Participants in the conversation")
    private List<UserResponse> participants;

    @Schema(description = "Last message in the conversation")
    private ChatMessageResponse lastMessage;

    @Schema(description = "Total unread message count for current user", example = "3")
    private Long unreadCount;

    @Schema(description = "Conversation creation timestamp", example = "2023-12-01T10:30:00Z")
    private ZonedDateTime createdAt;

    @Schema(description = "Last activity timestamp", example = "2023-12-01T10:30:00Z")
    private ZonedDateTime lastActivity;

    @Schema(description = "Conversation status", example = "ACTIVE")
    private ConversationStatus status;

    /**
     * Conversation status enum
     */
    public enum ConversationStatus {
        ACTIVE, ARCHIVED, BLOCKED
    }
}