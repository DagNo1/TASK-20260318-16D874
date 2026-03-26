package com.pettrade.practiceplatform.api.profile;

import jakarta.validation.constraints.NotBlank;

public record SensitiveProfileUpsertRequest(
        @NotBlank(message = "phoneNumber is required") String phoneNumber,
        @NotBlank(message = "idNumber is required") String idNumber
) {
}
