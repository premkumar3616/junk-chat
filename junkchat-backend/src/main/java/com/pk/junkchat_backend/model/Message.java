package com.pk.junkchat_backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "message")
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Column(nullable = false)
    private String content;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @Column(name = "hidden_for_user_ids")
    private Long[] hiddenForUserIds;

    @Column(name = "read_by_user_ids")
    private Long[] readByUserIds;

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public User getRecipient() {
        return recipient;
    }

    public void setRecipient(User recipient) {
        this.recipient = recipient;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public Long[] getHiddenForUserIds() {
        return hiddenForUserIds;
    }

    public void setHiddenForUserIds(Long[] hiddenForUserIds) {
        this.hiddenForUserIds = hiddenForUserIds;
    }

    public Long[] getReadByUserIds() {
        return readByUserIds;
    }

    public void setReadByUserIds(Long[] readByUserIds) {
        this.readByUserIds = readByUserIds;
    }
}