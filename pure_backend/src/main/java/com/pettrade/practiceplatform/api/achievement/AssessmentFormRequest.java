package com.pettrade.practiceplatform.api.achievement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AssessmentFormRequest(
        @NotBlank(message = "assessorName is required") String assessorName,
        @NotNull(message = "assessedAt is required") LocalDateTime assessedAt,
        @NotNull(message = "overallScore is required") BigDecimal overallScore,
        String strengths,
        String improvements,
        String recommendations,
        @NotBlank(message = "criteriaJson is required") String criteriaJson
) {
}
