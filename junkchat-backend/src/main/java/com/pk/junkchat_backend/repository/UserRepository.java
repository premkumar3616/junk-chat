package com.pk.junkchat_backend.repository;

import com.pk.junkchat_backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {
    Optional<User> findByUsernameOrEmail(String username, String email);

    Optional<User> findByEmail(String email);
    User findByUsernameIgnoreCaseOrEmailIgnoreCase(String username, String email);

    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) AND u.id != :currentUserId")
    List<User> findByUsernameContainingIgnoreCaseAndIdNot(String query, Long currentUserId);
}

