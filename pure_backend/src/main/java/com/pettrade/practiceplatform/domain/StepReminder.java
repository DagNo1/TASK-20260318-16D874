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

import java.time.LocalDateTime;

@Entity
@Table(name = "step_reminders")
public class StepReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "timer_id", nullable = false)
    private StepTimer timer;

    @Column(name = "offset_seconds_before_due", nullable = false)
    private Long offsetSecondsBeforeDue;

    @Column(name = "remind_at")
    private LocalDateTime remindAt;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false)
    private boolean triggered;

    @Column(name = "triggered_at")
    private LocalDateTime triggeredAt;

    public Long getId() {
        return id;
    }

    public StepTimer getTimer() {
        return timer;
    }

    public void setTimer(StepTimer timer) {
        this.timer = timer;
    }

    public Long getOffsetSecondsBeforeDue() {
        return offsetSecondsBeforeDue;
    }

    public void setOffsetSecondsBeforeDue(Long offsetSecondsBeforeDue) {
        this.offsetSecondsBeforeDue = offsetSecondsBeforeDue;
    }

    public LocalDateTime getRemindAt() {
        return remindAt;
    }

    public void setRemindAt(LocalDateTime remindAt) {
        this.remindAt = remindAt;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isTriggered() {
        return triggered;
    }

    public void setTriggered(boolean triggered) {
        this.triggered = triggered;
    }

    public LocalDateTime getTriggeredAt() {
        return triggeredAt;
    }

    public void setTriggeredAt(LocalDateTime triggeredAt) {
        this.triggeredAt = triggeredAt;
    }
}
