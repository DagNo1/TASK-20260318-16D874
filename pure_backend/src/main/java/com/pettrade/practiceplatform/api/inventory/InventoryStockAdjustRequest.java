package com.pettrade.practiceplatform.api.inventory;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record InventoryStockAdjustRequest(
        @NotNull(message = "newQuantity is required") @PositiveOrZero(message = "newQuantity must be >= 0") Long newQuantity,
        String reason
) {
}
