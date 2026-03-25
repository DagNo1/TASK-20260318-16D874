package com.pettrade.practiceplatform.domain;

import com.pettrade.practiceplatform.domain.enumtype.CheckpointType;
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

import java.time.LocalDateTime;

@Entity
@Table(name = "session_checkpoints")
public class SessionCheckpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private PracticeSession session;

    @Enumerated(EnumType.STRING)
    @Column(name = "checkpoint_type", nullable = false)
    private CheckpointType checkpointType;

    @Column(name = "snapshot_json", nullable = false, columnDefinition = "json")
    private String snapshotJson;

    @Column(name = "created_at", nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public PracticeSession getSession() {
        return session;
    }

    public void setSession(PracticeSession session) {
        this.session = session;
    }

    public CheckpointType getCheckpointType() {
        return checkpointType;
    }

    public void setCheckpointType(CheckpointType checkpointType) {
        this.checkpointType = checkpointType;
    }

    public String getSnapshotJson() {
        return snapshotJson;
    }

    public void setSnapshotJson(String snapshotJson) {
        this.snapshotJson = snapshotJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
