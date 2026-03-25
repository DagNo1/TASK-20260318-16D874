package com.pettrade.practiceplatform.api.im;

import com.pettrade.practiceplatform.domain.enumtype.ImMessageStatus;
import com.pettrade.practiceplatform.domain.enumtype.ImMessageType;

import java.time.LocalDateTime;

public record ImMessageEvent(
        String eventType,
        Long sessionId,
        Long messageId,
        Long senderId,
        String senderUsername,
        ImMessageType messageType,
        ImMessageStatus status,
        String text,
        String imageUrl,
        Integer foldedCount,
        LocalDateTime createdAt
) {
}
