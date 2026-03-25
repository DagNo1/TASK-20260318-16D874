package com.pettrade.practiceplatform.api.reporting;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReportAggregateView(
        Long aggregateId,
        String indicatorCode,
        Long organizationUserId,
        String businessDimension,
        String dimensionValue,
        LocalDateTime periodStart,
        LocalDateTime periodEnd,
        BigDecimal aggregatedValue
) {
}
