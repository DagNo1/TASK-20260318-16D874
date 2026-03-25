package com.pettrade.practiceplatform.api.practice;

import com.pettrade.practiceplatform.domain.enumtype.TimerAction;
import com.pettrade.practiceplatform.service.CheckpointService;
import com.pettrade.practiceplatform.service.CurrentUserService;
import com.pettrade.practiceplatform.service.PracticeSessionService;
import com.pettrade.practiceplatform.service.checkpoint.CheckpointTypeResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/practice/sessions")
@Tag(name = "Practice Sessions")
public class PracticeSessionController {

    private final PracticeSessionService practiceSessionService;
    private final CheckpointService checkpointService;
    private final CurrentUserService currentUserService;

    public PracticeSessionController(
            PracticeSessionService practiceSessionService,
            CheckpointService checkpointService,
            CurrentUserService currentUserService
    ) {
        this.practiceSessionService = practiceSessionService;
        this.checkpointService = checkpointService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','MERCHANT_OPERATOR','REGULAR_BUYER')")
    @Operation(summary = "Create a practice session")
    public ResponseEntity<PracticeSessionResponse> createSession(@Valid @RequestBody CreatePracticeSessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(practiceSessionService.createSession(request, currentUserService.currentUser()));
    }

    @GetMapping("/{sessionId}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','MERCHANT_OPERATOR','REGULAR_BUYER','REVIEWER')")
    @Operation(summary = "Get a practice session")
    public ResponseEntity<PracticeSessionResponse> getSession(@PathVariable Long sessionId) {
        return ResponseEntity.ok(practiceSessionService.getSession(sessionId, currentUserService.currentUser()));
    }

    @PostMapping("/{sessionId}/timers/{timerId}/command")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','MERCHANT_OPERATOR','REGULAR_BUYER')")
    @Operation(summary = "Command a timer: START/PAUSE/RESUME")
    public ResponseEntity<TimerStateResponse> commandTimer(
            @PathVariable Long sessionId,
            @PathVariable Long timerId,
            @Valid @RequestBody TimerCommandRequest request
    ) {
        TimerAction action = TimerAction.valueOf(request.action().toUpperCase());
        return ResponseEntity.ok(practiceSessionService.commandTimer(sessionId, timerId, action, currentUserService.currentUser()));
    }

    @PostMapping("/{sessionId}/steps/{stepId}/complete")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','MERCHANT_OPERATOR','REGULAR_BUYER')")
    @Operation(summary = "Mark step completed")
    public ResponseEntity<StepCompleteResponse> completeStep(@PathVariable Long sessionId, @PathVariable Long stepId) {
        return ResponseEntity.ok(practiceSessionService.completeStep(sessionId, stepId, currentUserService.currentUser()));
    }

    @PostMapping("/{sessionId}/checkpoints")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','MERCHANT_OPERATOR','REGULAR_BUYER')")
    @Operation(summary = "Save checkpoint")
    public ResponseEntity<CheckpointResponse> saveCheckpoint(
            @PathVariable Long sessionId,
            @RequestBody(required = false) CheckpointSaveRequest request
    ) {
        String reason = request == null ? null : request.reason();
        return ResponseEntity.ok(
                checkpointService.saveCheckpoint(
                        sessionId,
                        currentUserService.currentUser(),
                        CheckpointTypeResolver.fromReason(reason)
                )
        );
    }

    @GetMapping("/{sessionId}/checkpoints/latest")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','MERCHANT_OPERATOR','REGULAR_BUYER','REVIEWER')")
    @Operation(summary = "Load latest checkpoint")
    public ResponseEntity<CheckpointResponse> loadLatestCheckpoint(@PathVariable Long sessionId) {
        return ResponseEntity.ok(checkpointService.loadLatestCheckpoint(sessionId, currentUserService.currentUser()));
    }
}
