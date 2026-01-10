package com.pk.junkchat_backend.service;

import com.pk.junkchat_backend.model.Message;
import com.pk.junkchat_backend.model.User;
import com.pk.junkchat_backend.repository.MessageRepository;
import com.pk.junkchat_backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UserService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User findByIdentifier(String identifier) {
        User user = userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(identifier, identifier);
        logger.debug("findByIdentifier: identifier={}, user={}", identifier, user != null ? user.getUsername() : "null");
        return user;
    }

    public Optional<User> findById(Long id) {
        Optional<User> user = userRepository.findById(id);
        logger.debug("findById: id={}, user={}", id, user.isPresent() ? user.get().getUsername() : "null");
        return user;
    }

    public List<User> searchUsers(String query, Long currentUserId) {
        logger.info("Searching users with query: {}, currentUserId: {}", query, currentUserId);
        List<User> users = userRepository.findByUsernameContainingIgnoreCaseAndIdNot(query, currentUserId);
        logger.info("Found {} users for query: {}", users.size(), query);
        for (User user : users) {
            logger.debug("Processing user: id={}, username={}", user.getId(), user.getUsername());
            Message lastMessage = messageRepository.findLatestMessageBetweenUsers(currentUserId, user.getId());
            if (lastMessage != null) {
                user.setLastMessageContent(lastMessage.getContent());
                user.setLastMessageTime(lastMessage.getSentAt());
                logger.debug("Set last message for user {} (ID: {}): content='{}', time={}",
                        user.getUsername(), user.getId(), lastMessage.getContent(), lastMessage.getSentAt());
            } else {
                logger.debug("No last message found for user {} (ID: {}) with currentUserId={}",
                        user.getUsername(), user.getId(), currentUserId);
            }
        }
        return users;
    }

    public User updateUser(Long id, User updatedUser) {
        Optional<User> existingUser = userRepository.findById(id);
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            if (updatedUser.getUsername() != null && !updatedUser.getUsername().isEmpty()) {
                user.setUsername(updatedUser.getUsername());
            }
            if (updatedUser.getEmail() != null && !updatedUser.getEmail().isEmpty()) {
                user.setEmail(updatedUser.getEmail());
            }
            if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
                user.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
            }
            if (updatedUser.getProfilePic() != null && !updatedUser.getProfilePic().isEmpty()) {
                user.setProfilePic(updatedUser.getProfilePic());
            }
            User savedUser = userRepository.save(user);
            logger.debug("Updated user: id={}, username={}", savedUser.getId(), savedUser.getUsername());
            return savedUser;
        }
        logger.warn("User not found for update: id={}", id);
        return null;
    }

    public User registerUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User savedUser = userRepository.save(user);
        logger.debug("Registered user: id={}, username={}", savedUser.getId(), savedUser.getUsername());
        return savedUser;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = findByIdentifier(username);
        if (user == null) {
            logger.warn("User not found: username={}", username);
            throw new UsernameNotFoundException("User not found: " + username);
        }
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(), user.getPassword(), new ArrayList<>());
    }
}