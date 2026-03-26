package com.pettrade.practiceplatform.api.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

public record ProductSkuRequest(
        @NotBlank(message = "skuBarcode is required") String skuBarcode,
        @NotBlank(message = "sku name is required") String name,
        @NotNull(message = "stockQuantity is required") @PositiveOrZero(message = "stockQuantity must be >= 0") Long stockQuantity,
        @PositiveOrZero(message = "alertThreshold must be >= 0") Long alertThreshold,
        List<Long> attributeSpecIds
) {
}
