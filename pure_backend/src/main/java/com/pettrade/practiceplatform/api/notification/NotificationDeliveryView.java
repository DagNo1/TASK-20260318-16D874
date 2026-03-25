package com.pettrade.practiceplatform.api.notification;

import java.time.LocalDateTime;

public record NotificationDeliveryView(
        Long deliveryId,
        String eventType,
        String payloadJson,
        Long actorUserId,
        LocalDateTime deliveredAt,
        LocalDateTime readAt
) {
}
