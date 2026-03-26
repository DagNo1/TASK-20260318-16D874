package com.pettrade.practiceplatform.api.product;

import java.util.List;

public record ProductListResponse(
        List<ProductView> products
) {
}
