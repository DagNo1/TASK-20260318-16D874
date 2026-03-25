package com.pettrade.practiceplatform.api.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NotificationSubscriptionRequest(
        @NotBlank(message = "eventType is required") String eventType,
        @NotNull(message = "enabled is required") Boolean enabled
) {
}
