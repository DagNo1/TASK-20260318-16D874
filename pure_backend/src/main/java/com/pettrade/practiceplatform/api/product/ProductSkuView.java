package com.pettrade.practiceplatform.api.product;

import java.util.List;

public record ProductSkuView(
        Long id,
        String skuBarcode,
        String name,
        Long stockQuantity,
        Long alertThreshold,
        List<Long> attributeSpecIds
) {
}
