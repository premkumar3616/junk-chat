package com.pk.junkchat_backend.service;

import com.pk.junkchat_backend.repository.MessageRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class MessageCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(MessageCleanupService.class);

    @Autowired
    private MessageRepository messageRepository;

    @PostConstruct
    public void onStartup() {
        logger.info("MessageCleanupService initialized at {} - No cleanup triggered here", new java.util.Date());
    }

    @Scheduled(fixedRate = 24 * 60 * 60 * 1000) // 3 minutes in milliseconds for testing
    public void deleteOldMessages() {
        try {
            logger.info("Starting scheduled cleanup of messages older than 3 minutes at {}", new java.util.Date());
            int deletedCount = messageRepository.deleteMessagesOlderThan24Hours();
            logger.info("Completed scheduled cleanup of {} old messages", deletedCount);
        } catch (Exception ex) {
            logger.error("Error during scheduled cleanup: {}", ex.getMessage(), ex);
        }
    }

    // Add a method to catch external calls
    public void performCleanup() {
        logger.info("Performing manual cleanup of messages older than 3 minutes at {}", new java.util.Date());
        int deletedCount = messageRepository.deleteMessagesOlderThan24Hours();
        logger.info("Completed manual cleanup of {} old messages", deletedCount);
    }
}