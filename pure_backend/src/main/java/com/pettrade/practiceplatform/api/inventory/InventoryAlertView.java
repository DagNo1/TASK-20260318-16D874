package com.pettrade.practiceplatform.api.inventory;

import java.time.LocalDateTime;

public record InventoryAlertView(
        Long alertId,
        Long itemId,
        String sku,
        Long thresholdValue,
        Long stockQuantity,
        String status,
        String message,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt
) {
}
