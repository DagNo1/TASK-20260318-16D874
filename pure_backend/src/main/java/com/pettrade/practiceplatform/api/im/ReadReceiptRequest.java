package com.pettrade.practiceplatform.api.im;

import jakarta.validation.constraints.NotNull;

public record ReadReceiptRequest(@NotNull(message = "lastReadMessageId is required") Long lastReadMessageId) {
}
