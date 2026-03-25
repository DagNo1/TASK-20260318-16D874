package com.pettrade.practiceplatform.api.im;

import jakarta.validation.constraints.NotNull;

public record RecallMessageRequest(@NotNull(message = "messageId is required") Long messageId) {
}
