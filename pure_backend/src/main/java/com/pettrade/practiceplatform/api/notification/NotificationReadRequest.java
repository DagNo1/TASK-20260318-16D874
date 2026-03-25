package com.pettrade.practiceplatform.api.notification;

import jakarta.validation.constraints.NotNull;

public record NotificationReadRequest(@NotNull(message = "deliveryId is required") Long deliveryId) {
}
