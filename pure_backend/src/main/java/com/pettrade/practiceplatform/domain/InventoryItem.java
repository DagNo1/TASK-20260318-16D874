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
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_items")
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_user_id", nullable = false)
    private User merchantUser;

    @Column(nullable = false, length = 120)
    private String sku;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "stock_quantity", nullable = false)
    private Long stockQuantity;

    @Column(name = "alert_threshold")
    private Long alertThreshold;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public User getMerchantUser() {
        return merchantUser;
    }

    public void setMerchantUser(User merchantUser) {
        this.merchantUser = merchantUser;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Long stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public Long getAlertThreshold() {
        return alertThreshold;
    }

    public void setAlertThreshold(Long alertThreshold) {
        this.alertThreshold = alertThreshold;
    }
}
