package com.pk.junkchat_backend.service;

import com.pk.junkchat_backend.model.Message;
import com.pk.junkchat_backend.model.User;
import com.pk.junkchat_backend.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public Message sendMessage(User sender, User recipient, String content) {
        Message message = new Message();
        message.setSender(sender);
        message.setRecipient(recipient);
        message.setContent(content);
        message.setSentAt(LocalDateTime.now());
        Message savedMessage = messageRepository.save(message);

        // Publish to both sender and recipient
        messagingTemplate.convertAndSend("/topic/messages/" + sender.getId() + "/" + recipient.getId(), savedMessage);
        messagingTemplate.convertAndSend("/topic/messages/" + recipient.getId() + "/" + sender.getId(), savedMessage);

        return savedMessage;
    }

    public List<Message> getMessagesBetweenUsers(User user1, User user2) {
        return messageRepository.findMessagesBetweenUsers(user1.getId(), user2.getId());
    }
}