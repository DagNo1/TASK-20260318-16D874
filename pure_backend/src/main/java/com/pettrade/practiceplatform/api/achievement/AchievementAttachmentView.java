package com.pettrade.practiceplatform.api.achievement;

import java.time.LocalDateTime;

public record AchievementAttachmentView(
        Long attachmentId,
        Long attachmentVersion,
        String fileName,
        String fileUrl,
        String mimeType,
        LocalDateTime createdAt
) {
}
