package com.pettrade.practiceplatform.api.practice;

import jakarta.validation.constraints.NotBlank;

public record TimerCommandRequest(@NotBlank(message = "action is required") String action) {
}
