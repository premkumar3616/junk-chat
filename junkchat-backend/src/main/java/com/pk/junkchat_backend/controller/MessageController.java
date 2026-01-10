package com.pk.junkchat_backend.controller;

import com.pk.junkchat_backend.model.Message;
import com.pk.junkchat_backend.model.User;
import com.pk.junkchat_backend.repository.MessageRepository;
import com.pk.junkchat_backend.service.MessageService;
import com.pk.junkchat_backend.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class MessageController {

    private static final Logger logger = LoggerFactory.getLogger(MessageController.class);

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserService userService;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @GetMapping("/messages/{contactId}")
    public ResponseEntity<List<Message>> getMessages(@PathVariable Long contactId) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            logger.info("Fetching messages for username: {}, contactId: {}", username, contactId);
            User currentUser = userService.findByIdentifier(username);
            User contactUser = userService.findById(contactId).orElse(null);
            if (currentUser == null || contactUser == null) {
                logger.warn("User or contact not found: username={}, contactId={}", username, contactId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
            List<Message> messages = messageService.getMessagesBetweenUsers(currentUser, contactUser);
            logger.info("Returning {} messages for user: {}, contact: {}", messages.size(), username, contactUser.getUsername());
            return ResponseEntity.ok(messages);
        } catch (Exception ex) {
            logger.error("Error fetching messages: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/messages/mark-read/{contactId}")
    public ResponseEntity<?> markMessagesAsRead(@PathVariable Long contactId) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            logger.info("Marking messages as read for username: {}, contactId: {}", username, contactId);
            User currentUser = userService.findByIdentifier(username);
            User contactUser = userService.findById(contactId).orElse(null);
            if (currentUser == null || contactUser == null) {
                logger.warn("User or contact not found: username={}, contactId={}", username, contactId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
            messageRepository.markMessagesAsReadForUser(currentUser.getId(), contactId);
            logger.info("Messages marked as read for user: {}, contact: {}", username, contactUser.getUsername());

            // Update unread count for contact and notify via WebSocket
            User contactUpdate = new User();
            contactUpdate.setId(contactUser.getId());
            contactUpdate.setUsername(contactUser.getUsername());
            contactUpdate.setProfilePic(contactUser.getProfilePic());
            Message lastMessage = messageRepository.findLatestMessageBetweenUsers(currentUser.getId(), contactId);
            if (lastMessage != null) {
                contactUpdate.setLastMessageContent(lastMessage.getContent());
                contactUpdate.setLastMessageTime(lastMessage.getSentAt());
            }
            contactUpdate.setUnreadCount(getUnreadCount(currentUser.getId(), contactId));
            messagingTemplate.convertAndSend("/topic/messages/" + currentUser.getId(), contactUpdate);

            return ResponseEntity.ok("Messages marked as read");
        } catch (Exception ex) {
            logger.error("Error marking messages as read: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/messages")
    public ResponseEntity<Message> sendMessage(@RequestBody SendMessageRequest request) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            logger.info("Sending message from username: {} to recipientId: {}", username, request.getRecipientId());
            User sender = userService.findByIdentifier(username);
            User recipient = userService.findById(request.getRecipientId()).orElse(null);
            if (sender == null || recipient == null) {
                logger.warn("Sender or recipient not found: sender={}, recipientId={}", username, request.getRecipientId());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
            Message message = messageService.sendMessage(sender, recipient, request.getContent());
            logger.info("Message sent from {} to {}: content='{}'", sender.getUsername(), recipient.getUsername(), message.getContent());

            // Send WebSocket notifications for the message
            messagingTemplate.convertAndSend("/topic/messages/" + sender.getId() + "/" + recipient.getId(), message);
            messagingTemplate.convertAndSend("/topic/messages/" + recipient.getId() + "/" + sender.getId(), message);
            logger.debug("Sent message to /topic/messages/{}/{} and /topic/messages/{}/{}", sender.getId(), recipient.getId(), recipient.getId(), sender.getId());

            // Send WebSocket notifications for contact list updates
            User senderUpdate = new User();
            senderUpdate.setId(sender.getId());
            senderUpdate.setUsername(sender.getUsername());
            senderUpdate.setProfilePic(sender.getProfilePic());
            senderUpdate.setLastMessageContent(message.getContent());
            senderUpdate.setLastMessageTime(message.getSentAt());
            senderUpdate.setUnreadCount(0); // Sender sees their own message

            User recipientUpdate = new User();
            recipientUpdate.setId(recipient.getId());
            recipientUpdate.setUsername(recipient.getUsername());
            recipientUpdate.setProfilePic(recipient.getProfilePic());
            recipientUpdate.setLastMessageContent(message.getContent());
            recipientUpdate.setLastMessageTime(message.getSentAt());
            recipientUpdate.setUnreadCount(getUnreadCount(recipient.getId(), sender.getId()));

            messagingTemplate.convertAndSend("/topic/messages/" + sender.getId(), recipientUpdate);
            messagingTemplate.convertAndSend("/topic/messages/" + recipient.getId(), senderUpdate);
            logger.debug("Sent contact update to /topic/messages/{}: recipientId={}, content='{}', time={}, unreadCount={}",
                    sender.getId(), recipient.getId(), message.getContent(), message.getSentAt(), recipientUpdate.getUnreadCount());
            logger.debug("Sent contact update to /topic/messages/{}: senderId={}, content='{}', time={}, unreadCount={}",
                    recipient.getId(), sender.getId(), message.getContent(), message.getSentAt(), senderUpdate.getUnreadCount());

            return ResponseEntity.ok(message);
        } catch (Exception ex) {
            logger.error("Error sending message: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    private int getUnreadCount(Long recipientId, Long senderId) {
        List<Message> messages = messageService.getMessagesBetweenUsers(
                userService.findById(recipientId).orElse(null),
                userService.findById(senderId).orElse(null)
        );
        return (int) messages.stream()
                .filter(msg -> msg.getSender().getId().equals(senderId) &&
                        (msg.getReadByUserIds() == null || !containsUserId(msg.getReadByUserIds(), recipientId)))
                .count();
    }

    private boolean containsUserId(Long[] userIds, Long userId) {
        if (userIds == null) return false;
        for (Long id : userIds) {
            if (id != null && id.equals(userId)) return true;
        }
        return false;
    }

    static class SendMessageRequest {
        private Long recipientId;
        private String content;

        public Long getRecipientId() {
            return recipientId;
        }

        public void setRecipientId(Long recipientId) {
            this.recipientId = recipientId;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}