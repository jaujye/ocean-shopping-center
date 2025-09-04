package com.ocean.shopping.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

/**
 * WebSocket controller for real-time chat functionality
 */
@Controller
@Slf4j
public class ChatController {

    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public ChatMessage sendMessage(@Payload ChatMessage chatMessage) {
        log.debug("Received chat message: {}", chatMessage.getContent());
        return chatMessage;
    }

    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public ChatMessage addUser(@Payload ChatMessage chatMessage,
                               SimpMessageHeaderAccessor headerAccessor) {
        // Add username in web socket session
        headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());
        log.info("User {} joined the chat", chatMessage.getSender());
        return chatMessage;
    }

    // Simple chat message DTO
    public static class ChatMessage {
        private MessageType type;
        private String content;
        private String sender;

        public enum MessageType {
            CHAT, JOIN, LEAVE
        }

        // Constructors
        public ChatMessage() {}

        public ChatMessage(MessageType type, String content, String sender) {
            this.type = type;
            this.content = content;
            this.sender = sender;
        }

        // Getters and setters
        public MessageType getType() {
            return type;
        }

        public void setType(MessageType type) {
            this.type = type;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getSender() {
            return sender;
        }

        public void setSender(String sender) {
            this.sender = sender;
        }
    }
}