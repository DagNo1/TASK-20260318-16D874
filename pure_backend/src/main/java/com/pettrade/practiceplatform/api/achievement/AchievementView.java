package com.pettrade.practiceplatform.api.achievement;

import java.time.LocalDate;

public record AchievementView(
        Long achievementId,
        Long versionNo,
        String title,
        LocalDate periodStart,
        LocalDate periodEnd,
        String responsiblePerson,
        String conclusion
) {
}
