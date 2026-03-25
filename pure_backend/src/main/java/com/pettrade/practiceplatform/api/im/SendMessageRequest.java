package com.pettrade.practiceplatform.api.im;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SendMessageRequest(
        @NotBlank(message = "type is required") String type,
        String text,
        String imageFingerprint,
        String imageMimeType,
        @Positive(message = "imageSizeBytes must be positive") Long imageSizeBytes,
        String imageUrl,
        @NotNull(message = "clientMessageId is required") String clientMessageId
) {
}
