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
@Table(name = "step_node_reminders")
public class StepNodeReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "step_id", nullable = false)
    private PracticeStep step;

    @Column(name = "offset_seconds_after_step_start", nullable = false)
    private Long offsetSecondsAfterStepStart;

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

    public PracticeStep getStep() {
        return step;
    }

    public void setStep(PracticeStep step) {
        this.step = step;
    }

    public Long getOffsetSecondsAfterStepStart() {
        return offsetSecondsAfterStepStart;
    }

    public void setOffsetSecondsAfterStepStart(Long offsetSecondsAfterStepStart) {
        this.offsetSecondsAfterStepStart = offsetSecondsAfterStepStart;
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
