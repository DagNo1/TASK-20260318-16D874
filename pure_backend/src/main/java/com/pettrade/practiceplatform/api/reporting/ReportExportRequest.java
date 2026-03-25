package com.pettrade.practiceplatform.api.reporting;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ReportExportRequest(
        @NotBlank(message = "indicatorCode is required") String indicatorCode,
        @NotNull(message = "organizationUserId is required") Long organizationUserId,
        String businessDimension,
        @NotNull(message = "periodStart is required") LocalDateTime periodStart,
        @NotNull(message = "periodEnd is required") LocalDateTime periodEnd
) {
}
