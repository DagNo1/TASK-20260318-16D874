package com.pettrade.practiceplatform.api.im;

public record ImSessionEstablishedEvent(
        String eventType,
        Long sessionId,
        Long userId,
        String username,
        Long lastReadMessageId,
        long unreadCount
) {
}
