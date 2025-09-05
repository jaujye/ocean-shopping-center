package com.ocean.shopping.service;

import com.ocean.shopping.dto.chat.ChatMessageRequest;
import com.ocean.shopping.dto.chat.ChatMessageResponse;
import com.ocean.shopping.dto.chat.ConversationResponse;
import com.ocean.shopping.exception.BadRequestException;
import com.ocean.shopping.exception.ResourceNotFoundException;
import com.ocean.shopping.model.entity.ChatMessage;
import com.ocean.shopping.model.entity.User;
import com.ocean.shopping.repository.ChatMessageRepository;
import com.ocean.shopping.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing chat messages and conversations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Send a chat message
     */
    @Transactional
    public ChatMessageResponse sendMessage(UUID senderId, ChatMessageRequest request) {
        log.info("Sending message from user {} to user {}", senderId, request.getReceiverId());

        // Validate users exist
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found with id: " + senderId));
        
        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new ResourceNotFoundException("Receiver not found with id: " + request.getReceiverId()));

        // Validate message content
        if (request.getContent().trim().isEmpty()) {
            throw new BadRequestException("Message content cannot be empty");
        }

        // Generate conversation ID if not provided
        UUID conversationId = request.getConversationId();
        if (conversationId == null) {
            conversationId = generateConversationId(senderId, request.getReceiverId());
        }

        // Create and save message
        ChatMessage message = ChatMessage.builder()
                .conversationId(conversationId)
                .content(request.getContent().trim())
                .messageType(ChatMessage.MessageType.valueOf(request.getMessageType().name()))
                .sender(sender)
                .receiver(receiver)
                .attachmentUrl(request.getAttachmentUrl())
                .attachmentName(request.getAttachmentName())
                .deliveryStatus(ChatMessage.DeliveryStatus.SENT)
                .isRead(false)
                .build();

        ChatMessage savedMessage = chatMessageRepository.save(message);
        
        // Create response
        ChatMessageResponse response = ChatMessageResponse.fromEntity(savedMessage);

        // Send real-time notification to receiver
        sendRealTimeMessage(receiver.getId(), response);

        // Mark as delivered
        savedMessage.markAsDelivered();
        chatMessageRepository.save(savedMessage);

        log.info("Message sent successfully from {} to {}", sender.getEmail(), receiver.getEmail());
        return response;
    }

    /**
     * Get conversation history
     */
    @Transactional(readOnly = true)
    @PreAuthorize("@chatService.isParticipantInConversation(#userId, #conversationId)")
    public Page<ChatMessageResponse> getConversationHistory(UUID userId, UUID conversationId, Pageable pageable) {
        log.debug("Getting conversation history for user {} in conversation {}", userId, conversationId);

        Page<ChatMessage> messages = chatMessageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversationId, pageable);

        return messages.map(ChatMessageResponse::fromEntity);
    }

    /**
     * Get messages between two users
     */
    @Transactional(readOnly = true)
    @PreAuthorize("#userId == #user1Id or #userId == #user2Id")
    public Page<ChatMessageResponse> getMessagesBetweenUsers(UUID userId, UUID user1Id, UUID user2Id, Pageable pageable) {
        log.debug("Getting messages between users {} and {} for requesting user {}", user1Id, user2Id, userId);

        User user1 = userRepository.findById(user1Id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + user1Id));
        User user2 = userRepository.findById(user2Id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + user2Id));

        Page<ChatMessage> messages = chatMessageRepository
                .findMessagesBetweenUsers(user1, user2, pageable);

        return messages.map(ChatMessageResponse::fromEntity);
    }

    /**
     * Get user conversations
     */
    @Transactional(readOnly = true)
    public List<ConversationResponse> getUserConversations(UUID userId) {
        log.debug("Getting conversations for user {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        List<UUID> conversationIds = chatMessageRepository.findConversationsByUser(user);
        
        return conversationIds.stream()
                .map(conversationId -> buildConversationResponse(conversationId, user))
                .sorted((c1, c2) -> c2.getLastActivity().compareTo(c1.getLastActivity()))
                .collect(Collectors.toList());
    }

    /**
     * Mark message as read
     */
    @Transactional
    @PreAuthorize("@chatService.canReadMessage(#userId, #messageId)")
    public void markMessageAsRead(UUID userId, UUID messageId) {
        log.debug("Marking message {} as read by user {}", messageId, userId);

        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found with id: " + messageId));

        if (!message.getIsRead() && message.getReceiver().getId().equals(userId)) {
            message.markAsRead();
            chatMessageRepository.save(message);

            // Send read receipt to sender
            sendReadReceipt(message.getSender().getId(), messageId);
        }
    }

    /**
     * Mark all messages in conversation as read
     */
    @Transactional
    @PreAuthorize("@chatService.isParticipantInConversation(#userId, #conversationId)")
    public void markConversationAsRead(UUID userId, UUID conversationId) {
        log.debug("Marking all messages in conversation {} as read by user {}", conversationId, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        List<ChatMessage> unreadMessages = chatMessageRepository.findUnreadMessagesByReceiver(user)
                .stream()
                .filter(message -> message.getConversationId().equals(conversationId))
                .collect(Collectors.toList());

        ZonedDateTime now = ZonedDateTime.now();
        unreadMessages.forEach(message -> {
            message.setIsRead(true);
            message.setReadAt(now);
        });

        chatMessageRepository.saveAll(unreadMessages);
        log.info("Marked {} messages as read in conversation {}", unreadMessages.size(), conversationId);
    }

    /**
     * Get unread message count for user
     */
    @Transactional(readOnly = true)
    public long getUnreadMessageCount(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        return chatMessageRepository.countUnreadMessagesByReceiver(user);
    }

    /**
     * Delete message (soft delete - mark as system message)
     */
    @Transactional
    @PreAuthorize("@chatService.canDeleteMessage(#userId, #messageId)")
    public void deleteMessage(UUID userId, UUID messageId) {
        log.info("Deleting message {} by user {}", messageId, userId);

        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found with id: " + messageId));

        // Mark as deleted (soft delete)
        message.setContent("[Message deleted]");
        message.setMessageType(ChatMessage.MessageType.SYSTEM);
        message.setAttachmentUrl(null);
        message.setAttachmentName(null);

        chatMessageRepository.save(message);
    }

    /**
     * Create a system message
     */
    @Transactional
    public ChatMessageResponse createSystemMessage(UUID conversationId, String content) {
        log.debug("Creating system message in conversation {}", conversationId);

        // Get conversation participants
        List<User> participants = chatMessageRepository.findConversationParticipants(conversationId);
        if (participants.size() < 2) {
            throw new BadRequestException("Invalid conversation");
        }

        ChatMessage systemMessage = ChatMessage.builder()
                .conversationId(conversationId)
                .content(content)
                .messageType(ChatMessage.MessageType.SYSTEM)
                .sender(participants.get(0))
                .receiver(participants.get(1))
                .deliveryStatus(ChatMessage.DeliveryStatus.DELIVERED)
                .isRead(false)
                .build();

        ChatMessage saved = chatMessageRepository.save(systemMessage);
        
        // Notify all participants
        participants.forEach(participant -> 
            sendRealTimeMessage(participant.getId(), ChatMessageResponse.fromEntity(saved)));

        return ChatMessageResponse.fromEntity(saved);
    }

    // Security helper methods
    public boolean isParticipantInConversation(UUID userId, UUID conversationId) {
        List<User> participants = chatMessageRepository.findConversationParticipants(conversationId);
        return participants.stream().anyMatch(user -> user.getId().equals(userId));
    }

    public boolean canReadMessage(UUID userId, UUID messageId) {
        return chatMessageRepository.findById(messageId)
                .map(message -> message.getSender().getId().equals(userId) || 
                               message.getReceiver().getId().equals(userId))
                .orElse(false);
    }

    public boolean canDeleteMessage(UUID userId, UUID messageId) {
        return chatMessageRepository.findById(messageId)
                .map(message -> message.getSender().getId().equals(userId))
                .orElse(false);
    }

    // Private helper methods
    private UUID generateConversationId(UUID user1Id, UUID user2Id) {
        // Create deterministic conversation ID based on user IDs
        List<UUID> sortedIds = Arrays.asList(user1Id, user2Id);
        sortedIds.sort(UUID::compareTo);
        
        String combined = sortedIds.get(0).toString() + sortedIds.get(1).toString();
        return UUID.nameUUIDFromBytes(combined.getBytes());
    }

    private void sendRealTimeMessage(UUID userId, ChatMessageResponse message) {
        try {
            messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/chat",
                message
            );
            log.debug("Real-time message sent to user {}", userId);
        } catch (Exception e) {
            log.error("Failed to send real-time message to user {}: {}", userId, e.getMessage());
        }
    }

    private void sendReadReceipt(UUID senderId, UUID messageId) {
        try {
            Map<String, Object> receipt = Map.of(
                "type", "READ_RECEIPT",
                "messageId", messageId,
                "timestamp", ZonedDateTime.now()
            );
            
            messagingTemplate.convertAndSendToUser(
                senderId.toString(),
                "/queue/chat",
                receipt
            );
        } catch (Exception e) {
            log.error("Failed to send read receipt to user {}: {}", senderId, e.getMessage());
        }
    }

    private ConversationResponse buildConversationResponse(UUID conversationId, User currentUser) {
        // Get participants
        List<User> participants = chatMessageRepository.findConversationParticipants(conversationId);
        
        // Get latest message
        List<ChatMessage> latestMessages = chatMessageRepository
                .findLatestMessageInConversation(conversationId, PageRequest.of(0, 1));
        
        ChatMessageResponse lastMessage = latestMessages.isEmpty() ? null : 
                ChatMessageResponse.fromEntity(latestMessages.get(0));
        
        // Count unread messages
        long unreadCount = chatMessageRepository
                .countUnreadMessagesInConversation(conversationId, currentUser);
        
        return ConversationResponse.builder()
                .conversationId(conversationId)
                .participants(participants.stream()
                        .map(user -> com.ocean.shopping.dto.user.UserResponse.fromEntity(user))
                        .collect(Collectors.toList()))
                .lastMessage(lastMessage)
                .unreadCount(unreadCount)
                .lastActivity(lastMessage != null ? lastMessage.getCreatedAt() : ZonedDateTime.now())
                .createdAt(lastMessage != null ? lastMessage.getCreatedAt() : ZonedDateTime.now())
                .status(ConversationResponse.ConversationStatus.ACTIVE)
                .build();
    }
}