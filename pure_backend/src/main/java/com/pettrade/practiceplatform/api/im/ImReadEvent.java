package com.pettrade.practiceplatform.api.im;

import java.time.LocalDateTime;

public record ImReadEvent(
        String eventType,
        Long sessionId,
        Long userId,
        String username,
        Long lastReadMessageId,
        LocalDateTime readAt
) {
}
