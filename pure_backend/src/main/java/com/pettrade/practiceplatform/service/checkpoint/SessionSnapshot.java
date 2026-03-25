package com.pettrade.practiceplatform.service.checkpoint;

import com.pettrade.practiceplatform.domain.enumtype.SessionStatus;
import com.pettrade.practiceplatform.domain.enumtype.StepStatus;
import com.pettrade.practiceplatform.domain.enumtype.TimerState;

import java.util.List;

public record SessionSnapshot(
        Long sessionId,
        SessionStatus sessionStatus,
        List<StepSnapshot> steps
) {
    public record StepSnapshot(Long stepId, StepStatus status, List<TimerSnapshot> timers) {
    }

    public record TimerSnapshot(Long timerId, String timerKey, TimerState state, Long remainingSeconds, String dueAt) {
    }
}
