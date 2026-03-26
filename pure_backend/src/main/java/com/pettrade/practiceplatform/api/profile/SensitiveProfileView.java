package com.pettrade.practiceplatform.api.profile;

public record SensitiveProfileView(
        Long userId,
        String maskedPhoneNumber,
        String maskedIdNumber
) {
}
