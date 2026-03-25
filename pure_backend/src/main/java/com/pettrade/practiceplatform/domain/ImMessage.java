package com.pettrade.practiceplatform.domain;

import com.pettrade.practiceplatform.domain.enumtype.ImMessageStatus;
import com.pettrade.practiceplatform.domain.enumtype.ImMessageType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "im_messages")
public class ImMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ImSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 20)
    private ImMessageType messageType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ImMessageStatus status;

    @Column(columnDefinition = "text")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_asset_id")
    private ImImageAsset imageAsset;

    @Column(name = "dedup_key", length = 128)
    private String dedupKey;

    @Column(name = "folded_count", nullable = false)
    private Integer foldedCount;

    @Column(name = "last_folded_at")
    private LocalDateTime lastFoldedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public ImSession getSession() {
        return session;
    }

    public void setSession(ImSession session) {
        this.session = session;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public ImMessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(ImMessageType messageType) {
        this.messageType = messageType;
    }

    public ImMessageStatus getStatus() {
        return status;
    }

    public void setStatus(ImMessageStatus status) {
        this.status = status;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public ImImageAsset getImageAsset() {
        return imageAsset;
    }

    public void setImageAsset(ImImageAsset imageAsset) {
        this.imageAsset = imageAsset;
    }

    public String getDedupKey() {
        return dedupKey;
    }

    public void setDedupKey(String dedupKey) {
        this.dedupKey = dedupKey;
    }

    public Integer getFoldedCount() {
        return foldedCount;
    }

    public void setFoldedCount(Integer foldedCount) {
        this.foldedCount = foldedCount;
    }

    public LocalDateTime getLastFoldedAt() {
        return lastFoldedAt;
    }

    public void setLastFoldedAt(LocalDateTime lastFoldedAt) {
        this.lastFoldedAt = lastFoldedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
