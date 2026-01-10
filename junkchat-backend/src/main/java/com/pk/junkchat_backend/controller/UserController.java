package com.pk.junkchat_backend.controller;

import com.pk.junkchat_backend.model.Contact;
import com.pk.junkchat_backend.model.Message;
import com.pk.junkchat_backend.model.User;
import com.pk.junkchat_backend.repository.ContactRepository;
import com.pk.junkchat_backend.repository.MessageRepository;
import com.pk.junkchat_backend.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            logger.info("Fetching profile for username: {}", username);
            User user = userService.findByIdentifier(username);
            if (user == null) {
                logger.warn("User not found for username: {}", username);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            }
            return ResponseEntity.ok(user);
        } catch (Exception ex) {
            logger.error("Error fetching profile: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error fetching profile: " + ex.getMessage());
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody User updatedUser) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            logger.info("Updating profile for username: {}", username);
            User user = userService.findByIdentifier(username);
            if (user == null) {
                logger.warn("User not found for username: {}", username);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            }
            if (updatedUser.getProfilePic() != null && !updatedUser.getProfilePic().isEmpty()) {
                if (!updatedUser.getProfilePic().startsWith("data:image/")) {
                    logger.warn("Invalid profile picture format for username: {}", username);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid profile picture format");
                }
                if (updatedUser.getProfilePic().length() > 5 * 1024 * 1024) {
                    logger.warn("Profile picture too large for username: {}", username);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Profile picture size exceeds 5MB");
                }
            }
            User updated = userService.updateUser(user.getId(), updatedUser);
            if (updated != null) {
                List<Long> contactIds = contactRepository.findContactIdsByUserId(user.getId());
                for (Long contactId : contactIds) {
                    messagingTemplate.convertAndSend("/topic/contacts/" + contactId, updated);
                    logger.debug("Sent profile update to /topic/contacts/{}: username={}", contactId, updated.getUsername());
                }
                logger.info("Profile updated for username: {}", username);
                return ResponseEntity.ok(updated);
            }
            logger.warn("Profile update failed for username: {}", username);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Profile update failed");
        } catch (Exception ex) {
            logger.error("Error updating profile: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Profile update failed: " + ex.getMessage());
        }
    }

    @GetMapping("/users/search")
    public ResponseEntity<List<User>> searchUsers(@RequestParam String query) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.info("Searching users with query: {} for username: {}", query, currentUsername);
        User currentUser = userService.findByIdentifier(currentUsername);
        if (currentUser == null) {
            logger.warn("Current user not found: {}", currentUsername);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
        // Fetch pinned contacts
        List<User> pinnedContacts = contactRepository.findByUser(currentUser).stream()
                .map(Contact::getContact)
                .collect(Collectors.toList());
        // Fetch searched users
        List<User> searchedUsers = query.isEmpty() ? List.of() : userService.searchUsers(query, currentUser.getId());
        // Combine users
        Set<User> combinedUsers = new HashSet<>(pinnedContacts);
        combinedUsers.addAll(searchedUsers);

        // Set last message content and time for all users
        for (User user : combinedUsers) {
            Message lastMessage = messageRepository.findLatestMessageBetweenUsers(currentUser.getId(), user.getId());
            if (lastMessage != null) {
                user.setLastMessageContent(lastMessage.getContent());
                user.setLastMessageTime(lastMessage.getSentAt());
                logger.debug("Set last message for user {} (ID: {}): content='{}', time={}",
                        user.getUsername(), user.getId(), lastMessage.getContent(), lastMessage.getSentAt());
            } else {
                user.setLastMessageContent(null);
                user.setLastMessageTime(null);
                logger.debug("No last message found for user {} (ID: {}) with currentUserId={}",
                        user.getUsername(), user.getId(), currentUser.getId());
            }
        }

        logger.info("Returning {} users for query: {}", combinedUsers.size(), query);
        return ResponseEntity.ok(combinedUsers.stream().toList());
    }

    @PostMapping("/contacts")
    public ResponseEntity<?> addContact(@RequestBody AddContactRequest request) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            logger.info("Adding contact for username: {}, contact: {}", username, request.getUsername());
            User user = userService.findByIdentifier(username);
            if (user == null) {
                logger.warn("User not found: {}", username);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            }
            User contactUser = userService.findByIdentifier(request.getUsername());
            if (contactUser == null) {
                logger.warn("Contact user not found: {}", request.getUsername());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Contact user not found");
            }
            if (!contactRepository.existsByUserIdAndContactId(user.getId(), contactUser.getId())) {
                Contact contact = new Contact();
                contact.setUser(user);
                contact.setContact(contactUser);
                contactRepository.save(contact);
                Message lastMessage = messageRepository.findLatestMessageBetweenUsers(user.getId(), contactUser.getId());
                if (lastMessage != null) {
                    contactUser.setLastMessageContent(lastMessage.getContent());
                    contactUser.setLastMessageTime(lastMessage.getSentAt());
                    logger.debug("Set last message for contact {}: content='{}', time={}", contactUser.getUsername(), lastMessage.getContent(), lastMessage.getSentAt());
                } else {
                    logger.debug("No last message found for contact {} (ID: {})", contactUser.getUsername(), contactUser.getId());
                }
                messagingTemplate.convertAndSend("/topic/contacts/" + user.getId(), contactUser);
                logger.info("Contact added: {} for user: {}", contactUser.getUsername(), username);
            }
            return ResponseEntity.ok("Contact added successfully");
        } catch (Exception ex) {
            logger.error("Error adding contact: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error adding contact: " + ex.getMessage());
        }
    }

    @DeleteMapping("/contacts")
    public ResponseEntity<?> removeContact(@RequestBody RemoveContactRequest request) {
        try {
            if (request.getUsername() == null || request.getUsername().isEmpty()) {
                logger.warn("Invalid request: username is null or empty");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Username is required");
            }
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            logger.info("Removing contact for username: {}, contact: {}", username, request.getUsername());
            User user = userService.findByIdentifier(username);
            if (user == null) {
                logger.warn("User not found: {}", username);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            }
            User contactUser = userService.findByIdentifier(request.getUsername().toLowerCase());
            if (contactUser == null) {
                logger.warn("Contact user not found: {}", request.getUsername());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Contact user not found");
            }
            if (contactRepository.existsByUserIdAndContactId(user.getId(), contactUser.getId())) {
                contactRepository.deleteByUserIdAndContactId(user.getId(), contactUser.getId());
                messageRepository.hideMessagesForUser(user.getId(), contactUser.getId());
                messagingTemplate.convertAndSend("/topic/contacts/remove/" + user.getId(), contactUser);
                logger.info("Contact and messages hidden: {} for user: {}", contactUser.getUsername(), username);
                return ResponseEntity.ok("Contact and chat removed successfully");
            }
            logger.warn("Contact not found: {} for user: {}", request.getUsername(), username);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Contact not found in your list");
        } catch (Exception ex) {
            logger.error("Error removing contact: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error removing contact: " + ex.getMessage());
        }
    }

    static class AddContactRequest {
        private String username;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }
    }

    static class RemoveContactRequest {
        private String username;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }
    }
}