package com.pettrade.practiceplatform.api.approval;

import jakarta.validation.constraints.NotBlank;

public record ApprovalRequestCreateDto(
        @NotBlank(message = "requestType is required") String requestType,
        @NotBlank(message = "targetResource is required") String targetResource,
        @NotBlank(message = "targetAction is required") String targetAction,
        @NotBlank(message = "payloadJson is required") String payloadJson
) {
}
