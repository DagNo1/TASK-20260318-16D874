package com.pettrade.practiceplatform.api.approval;

public record AuditLogView(Long auditId, String eventType, Long actorUserId, String detailsJson) {
}
