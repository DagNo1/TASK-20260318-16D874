package com.pettrade.practiceplatform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pettrade.practiceplatform.api.practice.CheckpointResponse;
import com.pettrade.practiceplatform.domain.PracticeSession;
import com.pettrade.practiceplatform.domain.PracticeStep;
import com.pettrade.practiceplatform.domain.SessionCheckpoint;
import com.pettrade.practiceplatform.domain.StepTimer;
import com.pettrade.practiceplatform.domain.User;
import com.pettrade.practiceplatform.domain.enumtype.CheckpointType;
import com.pettrade.practiceplatform.domain.enumtype.TimerState;
import com.pettrade.practiceplatform.exception.NotFoundException;
import com.pettrade.practiceplatform.repository.PracticeSessionRepository;
import com.pettrade.practiceplatform.repository.PracticeStepRepository;
import com.pettrade.practiceplatform.repository.SessionCheckpointRepository;
import com.pettrade.practiceplatform.repository.StepTimerRepository;
import com.pettrade.practiceplatform.service.checkpoint.SessionSnapshot;
import com.pettrade.practiceplatform.service.timer.TimerStateMachine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CheckpointService {

    private final PracticeSessionRepository sessionRepository;
    private final PracticeStepRepository stepRepository;
    private final StepTimerRepository timerRepository;
    private final SessionCheckpointRepository checkpointRepository;
    private final ObjectMapper objectMapper;
    private final TimerStateMachine timerStateMachine;
    private final ReminderSchedulingService reminderSchedulingService;
    private final Clock clock;

    public CheckpointService(
            PracticeSessionRepository sessionRepository,
            PracticeStepRepository stepRepository,
            StepTimerRepository timerRepository,
            SessionCheckpointRepository checkpointRepository,
            ObjectMapper objectMapper,
            TimerStateMachine timerStateMachine,
            ReminderSchedulingService reminderSchedulingService,
            Clock clock
    ) {
        this.sessionRepository = sessionRepository;
        this.stepRepository = stepRepository;
        this.timerRepository = timerRepository;
        this.checkpointRepository = checkpointRepository;
        this.objectMapper = objectMapper;
        this.timerStateMachine = timerStateMachine;
        this.reminderSchedulingService = reminderSchedulingService;
        this.clock = clock;
    }

    @Transactional
    public CheckpointResponse saveCheckpoint(Long sessionId, User actor, CheckpointType type) {
        PracticeSession session = loadOwnedSessionForUpdate(sessionId, actor);
        SessionCheckpoint checkpoint = persistSnapshot(session, type);
        return toResponse(checkpoint);
    }

    @Transactional
    public CheckpointResponse loadLatestCheckpoint(Long sessionId, User actor) {
        PracticeSession session = loadOwnedSessionForUpdate(sessionId, actor);
        LocalDateTime now = LocalDateTime.now(clock);
        boolean changed = reconcileRunningTimers(sessionId);
        reminderSchedulingService.rescheduleForSession(sessionId, now);
        if (changed) {
            SessionCheckpoint fresh = persistSnapshot(session, CheckpointType.AUTO);
            return toResponse(fresh);
        }
        return checkpointRepository.findTopBySessionIdOrderByCreatedAtDesc(sessionId)
                .map(this::toResponse)
                .orElseGet(() -> toResponse(persistSnapshot(session, CheckpointType.MANUAL)));
    }

    @Transactional
    public void autoSaveForSession(PracticeSession session) {
        sessionRepository.findByIdForUpdate(session.getId())
                .ifPresent(locked -> {
                    reconcileRunningTimers(locked.getId());
                    persistSnapshot(locked, CheckpointType.AUTO);
                });
    }

    private PracticeSession loadOwnedSessionForUpdate(Long sessionId, User actor) {
        PracticeSession session = sessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new NotFoundException("Session not found"));
        if (!session.getUser().getId().equals(actor.getId())) {
            throw new NotFoundException("Session not found");
        }
        return session;
    }

    private boolean reconcileRunningTimers(Long sessionId) {
        List<StepTimer> timers = timerRepository.findByStepSessionId(sessionId);
        LocalDateTime now = LocalDateTime.now(clock);
        boolean changed = false;
        for (StepTimer timer : timers) {
            if (timer.getState() == TimerState.RUNNING) {
                changed = timerStateMachine.reconcileRunningTimer(timer, now) || changed;
            }
        }
        if (changed) {
            timerRepository.saveAll(timers);
        }
        return changed;
    }

    private SessionCheckpoint persistSnapshot(PracticeSession session, CheckpointType type) {
        SessionSnapshot snapshot = buildSnapshot(session.getId(), session.getStatus());
        SessionCheckpoint checkpoint = new SessionCheckpoint();
        checkpoint.setSession(session);
        checkpoint.setCheckpointType(type);
        checkpoint.setSnapshotJson(asJson(snapshot));
        return checkpointRepository.save(checkpoint);
    }

    private SessionSnapshot buildSnapshot(Long sessionId, com.pettrade.practiceplatform.domain.enumtype.SessionStatus status) {
        List<PracticeStep> steps = stepRepository.findBySessionIdOrderByStepOrderAsc(sessionId);
        List<SessionSnapshot.StepSnapshot> stepSnapshots = steps.stream().map(step -> {
            List<StepTimer> timers = timerRepository.findByStepId(step.getId());
            List<SessionSnapshot.TimerSnapshot> timerSnapshots = timers.stream().map(timer ->
                    new SessionSnapshot.TimerSnapshot(
                            timer.getId(),
                            timer.getTimerKey(),
                            timer.getState(),
                            timer.getRemainingSeconds(),
                            timer.getDueAt() == null ? null : timer.getDueAt().toString()
                    )
            ).toList();
            return new SessionSnapshot.StepSnapshot(step.getId(), step.getStatus(), timerSnapshots);
        }).toList();
        return new SessionSnapshot(sessionId, status, stepSnapshots);
    }

    private String asJson(SessionSnapshot snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize checkpoint snapshot", e);
        }
    }

    private CheckpointResponse toResponse(SessionCheckpoint checkpoint) {
        return new CheckpointResponse(
                checkpoint.getId(),
                checkpoint.getCheckpointType().name(),
                checkpoint.getCreatedAt(),
                checkpoint.getSnapshotJson()
        );
    }
}
