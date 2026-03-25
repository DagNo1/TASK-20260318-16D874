package com.pettrade.practiceplatform.api.approval;

import jakarta.validation.constraints.NotBlank;

public record ApprovalDecisionDto(
        @NotBlank(message = "decision is required") String decision,
        String comment
) {
}
