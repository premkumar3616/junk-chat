package com.pk.junkchat_backend.controller;

import com.pk.junkchat_backend.model.Contact;
import com.pk.junkchat_backend.model.User;
import com.pk.junkchat_backend.repository.ContactRepository;
import com.pk.junkchat_backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private ContactRepository contactRepository;

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userService.findByIdentifier(username);
            return ResponseEntity.ok(user);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found: " + ex.getMessage());
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody User updatedUser) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userService.findByIdentifier(username);
            return ResponseEntity.ok(userService.updateUser(user.getId(), updatedUser));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Profile update failed: " + ex.getMessage());
        }
    }

    @GetMapping("/contacts")
    public ResponseEntity<?> getContacts(@RequestParam(required = false) String search) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userService.findByIdentifier(username);
            List<Contact> contacts = contactRepository.findByUser(user);
            List<User> contactUsers = contacts.stream()
                    .map(Contact::getContact)
                    .filter(contact -> search == null || contact.getUsername().toLowerCase().contains(search.toLowerCase()))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(contactUsers);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error fetching contacts: " + ex.getMessage());
        }
    }

    @PostMapping("/contacts")
    public ResponseEntity<?> addContact(@RequestBody AddContactRequest request) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userService.findByIdentifier(username);
            User contactUser = userService.findByIdentifier(request.getUsername());
            if (contactUser == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Contact user not found");
            }
            Contact contact = new Contact();
            contact.setUser(user);
            contact.setContact(contactUser);
            contactRepository.save(contact);
            return ResponseEntity.ok("Contact added successfully");
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error adding contact: " + ex.getMessage());
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
}