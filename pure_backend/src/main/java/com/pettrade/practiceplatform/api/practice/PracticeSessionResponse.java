package com.pettrade.practiceplatform.api.practice;

import com.pettrade.practiceplatform.domain.enumtype.SessionStatus;
import com.pettrade.practiceplatform.domain.enumtype.StepStatus;
import com.pettrade.practiceplatform.domain.enumtype.TimerState;

import java.time.LocalDateTime;
import java.util.List;

public record PracticeSessionResponse(
        Long sessionId,
        String title,
        SessionStatus status,
        List<StepView> steps
) {
    public record StepView(
            Long stepId,
            Integer stepOrder,
            String name,
            String instruction,
            StepStatus status,
            List<TimerView> timers
    ) {
    }

    public record TimerView(
            Long timerId,
            String timerKey,
            TimerState state,
            Long durationSeconds,
            Long remainingSeconds,
            LocalDateTime dueAt
    ) {
    }
}
