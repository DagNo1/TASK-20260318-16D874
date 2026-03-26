package com.pettrade.practiceplatform.api.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ProductUpsertRequest(
        @NotBlank(message = "productCode is required") String productCode,
        @NotBlank(message = "name is required") String name,
        @NotNull(message = "brandId is required") Long brandId,
        @NotNull(message = "categoryId is required") Long categoryId,
        @NotEmpty(message = "skus are required") List<@Valid ProductSkuRequest> skus
) {
}
