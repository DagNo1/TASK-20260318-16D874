package com.pettrade.practiceplatform.api.inventory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record InventoryItemUpsertRequest(
        @NotBlank(message = "sku is required") String sku,
        @NotBlank(message = "name is required") String name,
        @NotNull(message = "stockQuantity is required") @PositiveOrZero(message = "stockQuantity must be >= 0") Long stockQuantity,
        @PositiveOrZero(message = "alertThreshold must be >= 0") Long alertThreshold
) {
}
