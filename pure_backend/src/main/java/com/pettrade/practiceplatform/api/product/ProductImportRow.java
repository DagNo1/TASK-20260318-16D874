package com.pettrade.practiceplatform.api.product;

public record ProductImportRow(
        String productCode,
        String productName,
        Long brandId,
        Long categoryId,
        String skuBarcode,
        String skuName,
        Long stockQuantity,
        Long alertThreshold
) {
}
