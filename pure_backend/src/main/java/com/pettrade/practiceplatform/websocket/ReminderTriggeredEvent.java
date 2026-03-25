package com.pettrade.practiceplatform.websocket;

import java.time.LocalDateTime;

public record ReminderTriggeredEvent(
        String type,
        Long sessionId,
        Long stepId,
        Long timerId,
        String timerKey,
        String message,
        LocalDateTime remindAt
) {
}
