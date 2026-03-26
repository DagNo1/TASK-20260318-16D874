package com.pettrade.practiceplatform.api.product;

import java.util.List;

public record ProductView(
        Long id,
        String productCode,
        String name,
        Long brandId,
        Long categoryId,
        boolean listed,
        List<ProductSkuView> skus
) {
}
