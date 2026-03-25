package com.pettrade.practiceplatform.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_logs")
public class InventoryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private InventoryItem item;

    @Column(name = "previous_quantity", nullable = false)
    private Long previousQuantity;

    @Column(name = "new_quantity", nullable = false)
    private Long newQuantity;

    @Column(name = "change_reason", length = 255)
    private String changeReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_user_id", nullable = false)
    private User changedByUser;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public InventoryItem getItem() {
        return item;
    }

    public void setItem(InventoryItem item) {
        this.item = item;
    }

    public Long getPreviousQuantity() {
        return previousQuantity;
    }

    public void setPreviousQuantity(Long previousQuantity) {
        this.previousQuantity = previousQuantity;
    }

    public Long getNewQuantity() {
        return newQuantity;
    }

    public void setNewQuantity(Long newQuantity) {
        this.newQuantity = newQuantity;
    }

    public String getChangeReason() {
        return changeReason;
    }

    public void setChangeReason(String changeReason) {
        this.changeReason = changeReason;
    }

    public User getChangedByUser() {
        return changedByUser;
    }

    public void setChangedByUser(User changedByUser) {
        this.changedByUser = changedByUser;
    }
}
