package com.pk.junkchat_backend.repository;

import com.pk.junkchat_backend.model.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import com.pk.junkchat_backend.model.User;

import java.util.List;


public interface ContactRepository extends JpaRepository<Contact,Long> {
    List<Contact> findByUser(User user);
}
