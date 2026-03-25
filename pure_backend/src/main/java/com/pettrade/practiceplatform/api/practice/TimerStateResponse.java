package com.pettrade.practiceplatform.api.practice;

import com.pettrade.practiceplatform.domain.enumtype.TimerState;

import java.time.LocalDateTime;

public record TimerStateResponse(
        Long timerId,
        String timerKey,
        TimerState state,
        Long remainingSeconds,
        LocalDateTime dueAt
) {
}
