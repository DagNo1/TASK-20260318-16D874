package com.pettrade.practiceplatform.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_sensitive_profiles")
public class UserSensitiveProfile {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "phone_number_encrypted")
    private String phoneNumberEncrypted;

    @Column(name = "id_number_encrypted")
    private String idNumberEncrypted;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Long getUserId() {
        return userId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getPhoneNumberEncrypted() {
        return phoneNumberEncrypted;
    }

    public void setPhoneNumberEncrypted(String phoneNumberEncrypted) {
        this.phoneNumberEncrypted = phoneNumberEncrypted;
    }

    public String getIdNumberEncrypted() {
        return idNumberEncrypted;
    }

    public void setIdNumberEncrypted(String idNumberEncrypted) {
        this.idNumberEncrypted = idNumberEncrypted;
    }
}
