package com.pettrade.practiceplatform.api.achievement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record AchievementUpsertRequest(
        @NotNull(message = "versionNo is required") @Positive(message = "versionNo must be > 0") Long versionNo,
        @NotBlank(message = "title is required") String title,
        @NotNull(message = "periodStart is required") LocalDate periodStart,
        @NotNull(message = "periodEnd is required") LocalDate periodEnd,
        @NotBlank(message = "responsiblePerson is required") String responsiblePerson,
        @NotBlank(message = "conclusion is required") String conclusion
) {
}
