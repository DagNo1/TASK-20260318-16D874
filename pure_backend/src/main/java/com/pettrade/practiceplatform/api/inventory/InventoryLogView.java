package com.pettrade.practiceplatform.api.inventory;

import java.time.LocalDateTime;

public record InventoryLogView(
        Long logId,
        Long itemId,
        String sku,
        Long previousQuantity,
        Long newQuantity,
        String reason,
        Long changedByUserId,
        LocalDateTime createdAt
) {
}
