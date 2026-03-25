package com.pettrade.practiceplatform.api.practice;

import com.pettrade.practiceplatform.domain.enumtype.StepStatus;

import java.time.LocalDateTime;

public record StepCompleteResponse(Long stepId, StepStatus status, LocalDateTime completedAt) {
}
