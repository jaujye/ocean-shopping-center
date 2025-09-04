package com.ocean.shopping.websocket;

import com.ocean.shopping.dto.chat.NotificationRequest;
import com.ocean.shopping.service.ChatService;
import com.ocean.shopping.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket event listener for managing connections, subscriptions, and user presence
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final SimpMessageSendingOperations messagingTemplate;
    private final ChatService chatService;
    private final NotificationService notificationService;
    
    // Store active connections: sessionId -> userId
    private final Map<String, UUID> activeConnections = new ConcurrentHashMap<>();
    
    // Store user presence: userId -> status
    private final Map<UUID, UserPresence> userPresenceMap = new ConcurrentHashMap<>();

    /**
     * Handle WebSocket connection established
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        try {
            StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
            String sessionId = headerAccessor.getSessionId();
            Principal principal = headerAccessor.getUser();
            
            if (principal != null) {
                UUID userId = extractUserIdFromPrincipal(principal);
                
                if (userId != null) {
                    // Store active connection
                    activeConnections.put(sessionId, userId);
                    
                    // Update user presence
                    UserPresence presence = userPresenceMap.computeIfAbsent(userId, 
                        k -> new UserPresence(userId, UserStatus.ONLINE));
                    presence.setStatus(UserStatus.ONLINE);
                    presence.setLastSeen(java.time.ZonedDateTime.now());
                    presence.addSession(sessionId);
                    
                    log.info("WebSocket connection established for user {} with session {}", userId, sessionId);
                    
                    // Send presence update to user's contacts
                    broadcastPresenceUpdate(userId, UserStatus.ONLINE);
                    
                    // Send pending notifications
                    sendPendingNotifications(userId);
                    
                } else {
                    log.warn("WebSocket connection established but could not extract user ID from principal: {}", 
                            principal.getName());
                }
            } else {
                log.warn("WebSocket connection established without authentication");
            }
            
        } catch (Exception e) {
            log.error("Error handling WebSocket connect event: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle WebSocket connection closed
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        try {
            StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
            String sessionId = headerAccessor.getSessionId();
            
            UUID userId = activeConnections.remove(sessionId);
            
            if (userId != null) {
                UserPresence presence = userPresenceMap.get(userId);
                if (presence != null) {
                    presence.removeSession(sessionId);
                    
                    // If no more active sessions, mark as offline
                    if (presence.getActiveSessions().isEmpty()) {
                        presence.setStatus(UserStatus.OFFLINE);
                        presence.setLastSeen(java.time.ZonedDateTime.now());
                        
                        // Send presence update to user's contacts
                        broadcastPresenceUpdate(userId, UserStatus.OFFLINE);
                        
                        log.info("User {} went offline (session: {})", userId, sessionId);
                    } else {
                        log.info("User {} disconnected session {} but still has active sessions", userId, sessionId);
                    }
                } else {
                    log.warn("No presence record found for user {} during disconnect", userId);
                }
                
                log.info("WebSocket connection closed for user {} with session {}", userId, sessionId);
                
                // Handle conversation cleanup if needed
                handleConversationCleanup(sessionId, headerAccessor);
                
            } else {
                log.warn("WebSocket connection closed but no user ID found for session: {}", sessionId);
            }
            
        } catch (Exception e) {
            log.error("Error handling WebSocket disconnect event: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle subscription events
     */
    @EventListener
    public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
        try {
            StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
            String destination = headerAccessor.getDestination();
            String sessionId = headerAccessor.getSessionId();
            Principal principal = headerAccessor.getUser();
            
            if (principal != null && StringUtils.hasText(destination)) {
                UUID userId = extractUserIdFromPrincipal(principal);
                
                log.debug("User {} subscribed to {} (session: {})", userId, destination, sessionId);
                
                // Handle specific subscription types
                if (destination.startsWith("/user/queue/chat")) {
                    handleChatSubscription(userId, sessionId);
                } else if (destination.startsWith("/user/queue/notifications")) {
                    handleNotificationSubscription(userId, sessionId);
                } else if (destination.startsWith("/topic/")) {
                    handleTopicSubscription(userId, destination, sessionId);
                }
            }
            
        } catch (Exception e) {
            log.error("Error handling WebSocket subscribe event: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle unsubscription events
     */
    @EventListener
    public void handleWebSocketUnsubscribeListener(SessionUnsubscribeEvent event) {
        try {
            StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
            String sessionId = headerAccessor.getSessionId();
            UUID userId = activeConnections.get(sessionId);
            
            if (userId != null) {
                log.debug("User {} unsubscribed (session: {})", userId, sessionId);
            }
            
        } catch (Exception e) {
            log.error("Error handling WebSocket unsubscribe event: {}", e.getMessage(), e);
        }
    }

    /**
     * Get user presence status
     */
    public UserPresence getUserPresence(UUID userId) {
        return userPresenceMap.get(userId);
    }

    /**
     * Get all online users
     */
    public Map<UUID, UserPresence> getOnlineUsers() {
        return userPresenceMap.entrySet().stream()
                .filter(entry -> entry.getValue().getStatus() == UserStatus.ONLINE)
                .collect(java.util.stream.Collectors.toMap(
                    Map.Entry::getKey, 
                    Map.Entry::getValue
                ));
    }

    /**
     * Check if user is online
     */
    public boolean isUserOnline(UUID userId) {
        UserPresence presence = userPresenceMap.get(userId);
        return presence != null && presence.getStatus() == UserStatus.ONLINE;
    }

    // Private helper methods
    
    private UUID extractUserIdFromPrincipal(Principal principal) {
        try {
            if (principal instanceof Authentication auth) {
                Object details = auth.getDetails();
                if (details instanceof Map<?, ?> detailsMap) {
                    Object userIdObj = detailsMap.get("userId");
                    if (userIdObj != null) {
                        return UUID.fromString(userIdObj.toString());
                    }
                }
            }
            
            // Fallback: try to parse principal name as UUID
            return UUID.fromString(principal.getName());
        } catch (Exception e) {
            log.error("Could not extract user ID from principal: {}", principal.getName(), e);
            return null;
        }
    }

    private void broadcastPresenceUpdate(UUID userId, UserStatus status) {
        try {
            Map<String, Object> presenceUpdate = Map.of(
                "type", "PRESENCE_UPDATE",
                "userId", userId,
                "status", status.name(),
                "timestamp", java.time.ZonedDateTime.now()
            );

            // Broadcast to user's contacts (this is a simplified implementation)
            // In a real application, you'd need to determine who should receive these updates
            messagingTemplate.convertAndSend("/topic/presence", presenceUpdate);
            
            log.debug("Broadcasted presence update for user {}: {}", userId, status);
            
        } catch (Exception e) {
            log.error("Error broadcasting presence update for user {}: {}", userId, e.getMessage());
        }
    }

    private void sendPendingNotifications(UUID userId) {
        try {
            // Trigger async processing of pending notifications for this user
            notificationService.processPendingNotifications();
            
        } catch (Exception e) {
            log.error("Error sending pending notifications to user {}: {}", userId, e.getMessage());
        }
    }

    private void handleConversationCleanup(String sessionId, StompHeaderAccessor headerAccessor) {
        try {
            Object conversationIdObj = headerAccessor.getSessionAttributes().get("conversationId");
            if (conversationIdObj != null) {
                String conversationId = conversationIdObj.toString();
                UUID userId = activeConnections.get(sessionId);
                
                if (userId != null) {
                    log.debug("Cleaning up conversation {} for user {} (session: {})", 
                            conversationId, userId, sessionId);
                    
                    // Create system message for leave if needed
                    UUID convId = UUID.fromString(conversationId);
                    chatService.createSystemMessage(convId, "User disconnected");
                }
            }
        } catch (Exception e) {
            log.error("Error during conversation cleanup: {}", e.getMessage());
        }
    }

    private void handleChatSubscription(UUID userId, String sessionId) {
        log.debug("User {} subscribed to chat messages", userId);
        // Additional logic for chat subscription if needed
    }

    private void handleNotificationSubscription(UUID userId, String sessionId) {
        log.debug("User {} subscribed to notifications", userId);
        
        // Send unread notification count
        try {
            long unreadCount = notificationService.getUnreadNotificationCount(userId);
            Map<String, Object> countUpdate = Map.of(
                "type", "UNREAD_COUNT",
                "count", unreadCount,
                "timestamp", java.time.ZonedDateTime.now()
            );
            
            messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/notifications",
                countUpdate
            );
        } catch (Exception e) {
            log.error("Error sending unread count to user {}: {}", userId, e.getMessage());
        }
    }

    private void handleTopicSubscription(UUID userId, String destination, String sessionId) {
        log.debug("User {} subscribed to topic: {}", userId, destination);
        // Handle topic subscriptions if needed
    }

    /**
     * User presence information
     */
    public static class UserPresence {
        private UUID userId;
        private UserStatus status;
        private java.time.ZonedDateTime lastSeen;
        private final java.util.Set<String> activeSessions = java.util.concurrent.ConcurrentHashMap.newKeySet();

        public UserPresence(UUID userId, UserStatus status) {
            this.userId = userId;
            this.status = status;
            this.lastSeen = java.time.ZonedDateTime.now();
        }

        // Getters and setters
        public UUID getUserId() { return userId; }
        public void setUserId(UUID userId) { this.userId = userId; }

        public UserStatus getStatus() { return status; }
        public void setStatus(UserStatus status) { this.status = status; }

        public java.time.ZonedDateTime getLastSeen() { return lastSeen; }
        public void setLastSeen(java.time.ZonedDateTime lastSeen) { this.lastSeen = lastSeen; }

        public java.util.Set<String> getActiveSessions() { return activeSessions; }
        
        public void addSession(String sessionId) { activeSessions.add(sessionId); }
        public void removeSession(String sessionId) { activeSessions.remove(sessionId); }
    }

    /**
     * User status enum
     */
    public enum UserStatus {
        ONLINE, OFFLINE, AWAY, BUSY
    }
}