package com.pk.junkchat_backend.controller;

import com.pk.junkchat_backend.model.Message;
import com.pk.junkchat_backend.model.User;
import com.pk.junkchat_backend.service.MessageService;
import com.pk.junkchat_backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<?> sendMessage(@RequestBody MessageRequest request) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User sender = userService.findByIdentifier(username);
            User recipient = userService.findById(request.getRecipientId()).orElse(null);
            if (recipient == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Recipient not found");
            }
            Message savedMessage = messageService.sendMessage(sender, recipient, request.getContent());
            return ResponseEntity.ok(savedMessage);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error sending message: " + ex.getMessage());
        }
    }

    @GetMapping("/{contactId}")
    public ResponseEntity<?> getMessages(@PathVariable Long contactId) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userService.findByIdentifier(username);
            User contact = userService.findById(contactId).orElse(null);
            if (contact == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Contact not found");
            }
            List<Message> messages = messageService.getMessagesBetweenUsers(user, contact);
            return ResponseEntity.ok(messages);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error fetching messages: " + ex.getMessage());
        }
    }

    static class MessageRequest {
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