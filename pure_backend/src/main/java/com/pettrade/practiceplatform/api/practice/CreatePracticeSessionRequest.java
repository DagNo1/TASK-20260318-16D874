package com.pettrade.practiceplatform.api.practice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record CreatePracticeSessionRequest(
        @NotBlank(message = "title is required") String title,
        @NotEmpty(message = "steps are required") List<@Valid StepInput> steps
) {
    public record StepInput(
            @NotBlank(message = "step name is required") String name,
            String instruction,
            Long estimatedSeconds,
            List<Long> techniqueCardIds,
            @NotEmpty(message = "timers are required") List<@Valid TimerInput> timers
    ) {
    }

    public record TimerInput(
            @NotBlank(message = "timer key is required") String timerKey,
            @NotNull(message = "durationSeconds is required") @Positive(message = "durationSeconds must be positive") Long durationSeconds,
            List<@Valid ReminderInput> reminders
    ) {
    }

    public record ReminderInput(
            @NotNull(message = "offsetSecondsBeforeDue is required") @Positive(message = "offsetSecondsBeforeDue must be positive") Long offsetSecondsBeforeDue,
            @NotBlank(message = "message is required") String message
    ) {
    }
}
