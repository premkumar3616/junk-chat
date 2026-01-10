package com.pk.junkchat_backend.repository;

import com.pk.junkchat_backend.model.Contact;
import com.pk.junkchat_backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {
    boolean existsByUserIdAndContactId(Long userId, Long contactId);

    List<Contact> findByUser(User user);

    @Query("SELECT c.contact.id FROM Contact c WHERE c.user.id = :userId")
    List<Long> findContactIdsByUserId(Long userId);

    @Query("SELECT c FROM Contact c WHERE c.user.id = :userId AND c.contact.id = :contactId")
    Optional<Contact> findByUserIdAndContactId(Long userId, Long contactId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Contact c WHERE c.user.id = :userId AND c.contact.id = :contactId")
    void deleteByUserIdAndContactId(Long userId, Long contactId);
}