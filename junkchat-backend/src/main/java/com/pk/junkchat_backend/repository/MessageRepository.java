package com.pk.junkchat_backend.repository;

import com.pk.junkchat_backend.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    @Query(value = "SELECT * FROM message WHERE ((sender_id = :userId1 AND recipient_id = :userId2) OR (sender_id = :userId2 AND recipient_id = :userId1)) AND (hidden_for_user_ids IS NULL OR NOT :userId1 = ANY(hidden_for_user_ids)) ORDER BY sent_at DESC LIMIT 1", nativeQuery = true)
    Message findLatestMessageBetweenUsers(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    @Query(value = "SELECT * FROM message WHERE ((sender_id = :userId1 AND recipient_id = :userId2) OR (sender_id = :userId2 AND recipient_id = :userId1)) AND (hidden_for_user_ids IS NULL OR NOT :userId1 = ANY(hidden_for_user_ids)) ORDER BY sent_at ASC", nativeQuery = true)
    List<Message> findMessagesBetweenUsers(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    @Modifying
    @Transactional
    @Query(value = "UPDATE message SET hidden_for_user_ids = COALESCE(hidden_for_user_ids, '{}') || ARRAY[:userId] WHERE ((sender_id = :userId AND recipient_id = :contactId) OR (sender_id = :contactId AND recipient_id = :userId)) AND NOT :userId = ANY(COALESCE(hidden_for_user_ids, '{}'))", nativeQuery = true)
    void hideMessagesForUser(@Param("userId") Long userId, @Param("contactId") Long contactId);

    @Modifying
    @Transactional
    @Query(value = "UPDATE message SET read_by_user_ids = COALESCE(read_by_user_ids, '{}') || ARRAY[:userId] WHERE ((sender_id = :contactId AND recipient_id = :userId) OR (sender_id = :userId AND recipient_id = :contactId)) AND NOT :userId = ANY(COALESCE(read_by_user_ids, '{}'))", nativeQuery = true)
    void markMessagesAsReadForUser(@Param("userId") Long userId, @Param("contactId") Long contactId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM message WHERE sent_at < CURRENT_TIMESTAMP - INTERVAL '24 hours'", nativeQuery = true)
    int deleteMessagesOlderThan24Hours();
}