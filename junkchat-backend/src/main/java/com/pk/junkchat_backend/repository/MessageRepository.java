package com.pk.junkchat_backend.repository;

import com.pk.junkchat_backend.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("SELECT m FROM Message m WHERE " +
            "(m.sender.id = :userId AND m.recipient.id = :contactId) OR " +
            "(m.sender.id = :contactId AND m.recipient.id = :userId) " +
            "ORDER BY m.sentAt ASC")
    List<Message> findMessagesBetweenUsers(@Param("userId") Long userId, @Param("contactId") Long contactId);
}