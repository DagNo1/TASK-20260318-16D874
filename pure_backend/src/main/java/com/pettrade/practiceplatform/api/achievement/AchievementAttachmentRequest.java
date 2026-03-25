package com.pettrade.practiceplatform.api.achievement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AchievementAttachmentRequest(
        @NotNull(message = "attachmentVersion is required") @Positive(message = "attachmentVersion must be > 0") Long attachmentVersion,
        @NotBlank(message = "fileName is required") String fileName,
        @NotBlank(message = "fileUrl is required") String fileUrl,
        @NotBlank(message = "mimeType is required") String mimeType
) {
}
