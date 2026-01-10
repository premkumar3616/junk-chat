package com.pk.junkchat_backend.service;

import com.pk.junkchat_backend.model.Contact;
import com.pk.junkchat_backend.model.Message;
import com.pk.junkchat_backend.model.User;
import com.pk.junkchat_backend.repository.ContactRepository;
import com.pk.junkchat_backend.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ContactRepository contactRepository;

    public Message sendMessage(User sender, User recipient, String content) {
        Message message = new Message();
        message.setSender(sender);
        message.setRecipient(recipient);
        message.setContent(content);
        message.setSentAt(LocalDateTime.now(ZoneId.of("UTC"))); // Use UTC
        Message savedMessage = messageRepository.save(message);

        // Add contacts for both sender and recipient
        boolean senderContactAdded = false;
        boolean recipientContactAdded = false;

        if (!contactRepository.existsByUserIdAndContactId(sender.getId(), recipient.getId())) {
            Contact senderContact = new Contact();
            senderContact.setUser(sender);
            senderContact.setContact(recipient);
            contactRepository.save(senderContact);
            senderContactAdded = true;
        }
        if (!contactRepository.existsByUserIdAndContactId(recipient.getId(), sender.getId())) {
            Contact recipientContact = new Contact();
            recipientContact.setUser(recipient);
            recipientContact.setContact(sender);
            contactRepository.save(recipientContact);
            recipientContactAdded = true;
        }

        // Publish contact update notifications
        if (senderContactAdded) {
            messagingTemplate.convertAndSend("/topic/contacts/" + sender.getId(), recipient);
        }
        if (recipientContactAdded) {
            messagingTemplate.convertAndSend("/topic/contacts/" + recipient.getId(), sender);
        }

        // Publish message to both sender and recipient
        messagingTemplate.convertAndSend("/topic/messages/" + sender.getId() + "/" + recipient.getId(), savedMessage);
        messagingTemplate.convertAndSend("/topic/messages/" + recipient.getId() + "/" + sender.getId(), savedMessage);

        return savedMessage;
    }

    public List<Message> getMessagesBetweenUsers(User user1, User user2) {
        List<Message> messages = messageRepository.findMessagesBetweenUsers(user1.getId(), user2.getId());
        return messages.stream()
                .filter(message -> message.getHiddenForUserIds() == null ||
                        !containsUserId(message.getHiddenForUserIds(), user1.getId()))
                .collect(Collectors.toList());
    }

    private boolean containsUserId(Long[] userIds, Long userId) {
        if (userIds == null) return false;
        for (Long id : userIds) {
            if (id != null && id.equals(userId)) return true;
        }
        return false;
    }
}