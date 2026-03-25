package com.pettrade.practiceplatform.domain;

import com.pettrade.practiceplatform.domain.enumtype.TimerState;
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
import jakarta.persistence.Version;

import java.time.LocalDateTime;

@Entity
@Table(name = "step_timers")
public class StepTimer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "step_id", nullable = false)
    private PracticeStep step;

    @Column(name = "timer_key", nullable = false, length = 120)
    private String timerKey;

    @Column(name = "duration_seconds", nullable = false)
    private Long durationSeconds;

    @Column(name = "remaining_seconds", nullable = false)
    private Long remainingSeconds;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private TimerState state;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "last_resumed_at")
    private LocalDateTime lastResumedAt;

    @Column(name = "last_paused_at")
    private LocalDateTime lastPausedAt;

    @Column(name = "due_at")
    private LocalDateTime dueAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    public Long getId() {
        return id;
    }

    public PracticeStep getStep() {
        return step;
    }

    public void setStep(PracticeStep step) {
        this.step = step;
    }

    public String getTimerKey() {
        return timerKey;
    }

    public void setTimerKey(String timerKey) {
        this.timerKey = timerKey;
    }

    public Long getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public Long getRemainingSeconds() {
        return remainingSeconds;
    }

    public void setRemainingSeconds(Long remainingSeconds) {
        this.remainingSeconds = remainingSeconds;
    }

    public TimerState getState() {
        return state;
    }

    public void setState(TimerState state) {
        this.state = state;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getLastResumedAt() {
        return lastResumedAt;
    }

    public void setLastResumedAt(LocalDateTime lastResumedAt) {
        this.lastResumedAt = lastResumedAt;
    }

    public LocalDateTime getLastPausedAt() {
        return lastPausedAt;
    }

    public void setLastPausedAt(LocalDateTime lastPausedAt) {
        this.lastPausedAt = lastPausedAt;
    }

    public LocalDateTime getDueAt() {
        return dueAt;
    }

    public void setDueAt(LocalDateTime dueAt) {
        this.dueAt = dueAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
