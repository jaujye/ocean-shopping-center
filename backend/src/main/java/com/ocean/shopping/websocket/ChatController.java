package com.ocean.shopping.websocket;

import com.ocean.shopping.dto.chat.ChatMessageRequest;
import com.ocean.shopping.dto.chat.ChatMessageResponse;
import com.ocean.shopping.dto.chat.NotificationRequest;
import com.ocean.shopping.exception.BadRequestException;
import com.ocean.shopping.exception.ResourceNotFoundException;
import com.ocean.shopping.service.ChatService;
import com.ocean.shopping.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

/**
 * WebSocket controller for real-time chat functionality
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;
    private final NotificationService notificationService;

    /**
     * Handle direct message sending
     */
    @MessageMapping("/chat.sendMessage")
    @SendToUser("/queue/reply")
    public ChatMessageResponse sendMessage(@Payload ChatMessageRequest request, Principal principal) {
        try {
            log.debug("Processing message from user: {}", principal.getName());
            
            UUID senderId = getUserIdFromPrincipal(principal);
            ChatMessageResponse response = chatService.sendMessage(senderId, request);
            
            log.debug("Message sent successfully from user {} to user {}", 
                    senderId, request.getReceiverId());
            return response;
            
        } catch (Exception e) {
            log.error("Error sending message: {}", e.getMessage());
            throw new BadRequestException("Failed to send message: " + e.getMessage());
        }
    }

    /**
     * Handle user joining a conversation
     */
    @MessageMapping("/chat.join/{conversationId}")
    public void joinConversation(@DestinationVariable String conversationId, 
                                Principal principal,
                                SimpMessageHeaderAccessor headerAccessor) {
        try {
            UUID userId = getUserIdFromPrincipal(principal);
            UUID convId = UUID.fromString(conversationId);
            
            // Verify user can join this conversation
            if (!chatService.isParticipantInConversation(userId, convId)) {
                throw new BadRequestException("User not authorized to join this conversation");
            }
            
            // Store conversation ID in session
            headerAccessor.getSessionAttributes().put("conversationId", conversationId);
            headerAccessor.getSessionAttributes().put("userId", userId.toString());
            
            log.info("User {} joined conversation {}", userId, conversationId);
            
            // Create system message for join
            chatService.createSystemMessage(convId, "User joined the conversation");
            
        } catch (Exception e) {
            log.error("Error joining conversation: {}", e.getMessage());
            throw new BadRequestException("Failed to join conversation: " + e.getMessage());
        }
    }

    /**
     * Handle user leaving a conversation
     */
    @MessageMapping("/chat.leave")
    public void leaveConversation(Principal principal,
                                 SimpMessageHeaderAccessor headerAccessor) {
        try {
            String conversationId = (String) headerAccessor.getSessionAttributes().get("conversationId");
            UUID userId = getUserIdFromPrincipal(principal);
            
            if (StringUtils.hasText(conversationId)) {
                UUID convId = UUID.fromString(conversationId);
                
                // Create system message for leave
                chatService.createSystemMessage(convId, "User left the conversation");
                
                log.info("User {} left conversation {}", userId, conversationId);
            }
            
            // Clean up session
            headerAccessor.getSessionAttributes().remove("conversationId");
            headerAccessor.getSessionAttributes().remove("userId");
            
        } catch (Exception e) {
            log.error("Error leaving conversation: {}", e.getMessage());
        }
    }

    /**
     * Handle marking messages as read
     */
    @MessageMapping("/chat.markAsRead")
    @SendToUser("/queue/reply")
    public Map<String, Object> markMessageAsRead(@Payload Map<String, String> request, 
                                                Principal principal) {
        try {
            UUID userId = getUserIdFromPrincipal(principal);
            String messageIdStr = request.get("messageId");
            String conversationIdStr = request.get("conversationId");
            
            if (StringUtils.hasText(messageIdStr)) {
                // Mark single message as read
                UUID messageId = UUID.fromString(messageIdStr);
                chatService.markMessageAsRead(userId, messageId);
                
                return Map.of(
                    "type", "MESSAGE_READ_SUCCESS",
                    "messageId", messageId,
                    "timestamp", java.time.ZonedDateTime.now()
                );
            } else if (StringUtils.hasText(conversationIdStr)) {
                // Mark entire conversation as read
                UUID conversationId = UUID.fromString(conversationIdStr);
                chatService.markConversationAsRead(userId, conversationId);
                
                return Map.of(
                    "type", "CONVERSATION_READ_SUCCESS",
                    "conversationId", conversationId,
                    "timestamp", java.time.ZonedDateTime.now()
                );
            } else {
                throw new BadRequestException("Either messageId or conversationId must be provided");
            }
            
        } catch (Exception e) {
            log.error("Error marking message as read: {}", e.getMessage());
            return Map.of(
                "type", "READ_ERROR",
                "error", e.getMessage(),
                "timestamp", java.time.ZonedDateTime.now()
            );
        }
    }

    /**
     * Handle typing indicators
     */
    @MessageMapping("/chat.typing/{conversationId}")
    public void handleTyping(@DestinationVariable String conversationId,
                           @Payload Map<String, Object> typingData,
                           Principal principal) {
        try {
            UUID userId = getUserIdFromPrincipal(principal);
            UUID convId = UUID.fromString(conversationId);
            
            // Verify user can type in this conversation
            if (!chatService.isParticipantInConversation(userId, convId)) {
                throw new BadRequestException("User not authorized for this conversation");
            }
            
            boolean isTyping = (Boolean) typingData.getOrDefault("isTyping", false);
            
            log.debug("User {} typing status in conversation {}: {}", userId, conversationId, isTyping);
            
            // Note: In a full implementation, you'd broadcast typing indicators
            // to other participants in the conversation
            
        } catch (Exception e) {
            log.error("Error handling typing indicator: {}", e.getMessage());
        }
    }

    /**
     * Handle connection status updates
     */
    @MessageMapping("/chat.status")
    @SendToUser("/queue/reply")
    public Map<String, Object> updateStatus(@Payload Map<String, String> statusData,
                                           Principal principal) {
        try {
            UUID userId = getUserIdFromPrincipal(principal);
            String status = statusData.get("status"); // "online", "away", "busy", "offline"
            
            log.debug("User {} status updated to: {}", userId, status);
            
            return Map.of(
                "type", "STATUS_UPDATED",
                "userId", userId,
                "status", status,
                "timestamp", java.time.ZonedDateTime.now()
            );
            
        } catch (Exception e) {
            log.error("Error updating status: {}", e.getMessage());
            return Map.of(
                "type", "STATUS_ERROR",
                "error", e.getMessage(),
                "timestamp", java.time.ZonedDateTime.now()
            );
        }
    }

    /**
     * Handle notification acknowledgment
     */
    @MessageMapping("/notification.ack")
    @SendToUser("/queue/reply")
    public Map<String, Object> acknowledgeNotification(@Payload Map<String, String> ackData,
                                                      Principal principal) {
        try {
            UUID userId = getUserIdFromPrincipal(principal);
            String notificationIdStr = ackData.get("notificationId");
            
            if (StringUtils.hasText(notificationIdStr)) {
                UUID notificationId = UUID.fromString(notificationIdStr);
                notificationService.markNotificationAsRead(userId, notificationId);
                
                return Map.of(
                    "type", "NOTIFICATION_ACK_SUCCESS",
                    "notificationId", notificationId,
                    "timestamp", java.time.ZonedDateTime.now()
                );
            } else {
                throw new BadRequestException("Notification ID is required");
            }
            
        } catch (Exception e) {
            log.error("Error acknowledging notification: {}", e.getMessage());
            return Map.of(
                "type", "NOTIFICATION_ACK_ERROR",
                "error", e.getMessage(),
                "timestamp", java.time.ZonedDateTime.now()
            );
        }
    }

    /**
     * Handle emergency system notifications (Admin only)
     */
    @MessageMapping("/admin.broadcast")
    @SendToUser("/queue/reply")
    public Map<String, Object> broadcastSystemNotification(@Payload NotificationRequest request,
                                                          Principal principal) {
        try {
            // Note: Authorization would be checked in the service layer
            notificationService.sendSystemAnnouncement(request);
            
            log.info("System announcement broadcasted by admin: {}", principal.getName());
            
            return Map.of(
                "type", "BROADCAST_SUCCESS",
                "message", "System announcement sent successfully",
                "timestamp", java.time.ZonedDateTime.now()
            );
            
        } catch (Exception e) {
            log.error("Error broadcasting system notification: {}", e.getMessage());
            return Map.of(
                "type", "BROADCAST_ERROR",
                "error", e.getMessage(),
                "timestamp", java.time.ZonedDateTime.now()
            );
        }
    }

    /**
     * Extract user ID from authentication principal
     */
    private UUID getUserIdFromPrincipal(Principal principal) {
        if (principal instanceof Authentication auth) {
            Object details = auth.getDetails();
            if (details instanceof Map<?, ?> detailsMap) {
                Object userIdObj = detailsMap.get("userId");
                if (userIdObj != null) {
                    return UUID.fromString(userIdObj.toString());
                }
            }
            
            // Fallback: try to parse principal name as UUID
            try {
                return UUID.fromString(principal.getName());
            } catch (IllegalArgumentException e) {
                // If principal name is not a UUID, we need to resolve it differently
                // This would depend on your authentication setup
                log.error("Could not extract user ID from principal: {}", principal.getName());
                throw new BadRequestException("Invalid user authentication");
            }
        }
        
        throw new BadRequestException("Invalid authentication principal");
    }
}