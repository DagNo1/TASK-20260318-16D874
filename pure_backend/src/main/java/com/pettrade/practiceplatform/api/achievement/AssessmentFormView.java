package com.pettrade.practiceplatform.api.achievement;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AssessmentFormView(
        Long formId,
        String assessorName,
        LocalDateTime assessedAt,
        BigDecimal overallScore,
        String strengths,
        String improvements,
        String recommendations,
        String criteriaJson
) {
}
