package com.pettrade.practiceplatform.api.inventory;

public record InventoryItemView(
        Long itemId,
        String sku,
        String name,
        Long stockQuantity,
        Long alertThreshold
) {
}
