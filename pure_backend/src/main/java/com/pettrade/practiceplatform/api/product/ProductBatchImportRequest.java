package com.pettrade.practiceplatform.api.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ProductBatchImportRequest(
        @NotEmpty(message = "rows are required") List<@Valid ProductImportRow> rows
) {
}
