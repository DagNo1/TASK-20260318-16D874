package com.pettrade.practiceplatform.api.approval;

import java.time.LocalDateTime;

public record ApprovalRequestView(
        Long requestId,
        String requestType,
        String status,
        String targetResource,
        String targetAction,
        String payloadJson,
        Long requestedByUserId,
        LocalDateTime createdAt,
        LocalDateTime decidedAt,
        LocalDateTime executedAt
) {
}
