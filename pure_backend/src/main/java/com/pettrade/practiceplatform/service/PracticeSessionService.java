package com.pettrade.practiceplatform.service;

import com.pettrade.practiceplatform.api.practice.CreatePracticeSessionRequest;
import com.pettrade.practiceplatform.api.practice.PracticeSessionResponse;
import com.pettrade.practiceplatform.api.practice.StepCompleteResponse;
import com.pettrade.practiceplatform.api.practice.TimerStateResponse;
import com.pettrade.practiceplatform.domain.PracticeSession;
import com.pettrade.practiceplatform.domain.PracticeStep;
import com.pettrade.practiceplatform.domain.StepReminder;
import com.pettrade.practiceplatform.domain.StepTimer;
import com.pettrade.practiceplatform.domain.TechniqueCard;
import com.pettrade.practiceplatform.domain.User;
import com.pettrade.practiceplatform.domain.enumtype.CheckpointType;
import com.pettrade.practiceplatform.domain.enumtype.SessionStatus;
import com.pettrade.practiceplatform.domain.enumtype.StepStatus;
import com.pettrade.practiceplatform.domain.enumtype.TimerAction;
import com.pettrade.practiceplatform.domain.enumtype.TimerState;
import com.pettrade.practiceplatform.exception.NotFoundException;
import com.pettrade.practiceplatform.repository.PracticeSessionRepository;
import com.pettrade.practiceplatform.repository.PracticeStepRepository;
import com.pettrade.practiceplatform.repository.StepReminderRepository;
import com.pettrade.practiceplatform.repository.StepTimerRepository;
import com.pettrade.practiceplatform.repository.TechniqueCardRepository;
import com.pettrade.practiceplatform.service.timer.TimerStateMachine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PracticeSessionService {

    private final PracticeSessionRepository sessionRepository;
    private final PracticeStepRepository stepRepository;
    private final StepTimerRepository timerRepository;
    private final StepReminderRepository reminderRepository;
    private final TechniqueCardRepository techniqueCardRepository;
    private final TimerStateMachine timerStateMachine;
    private final CheckpointService checkpointService;
    private final Clock clock;

    public PracticeSessionService(
            PracticeSessionRepository sessionRepository,
            PracticeStepRepository stepRepository,
            StepTimerRepository timerRepository,
            StepReminderRepository reminderRepository,
            TechniqueCardRepository techniqueCardRepository,
            TimerStateMachine timerStateMachine,
            CheckpointService checkpointService,
            Clock clock
    ) {
        this.sessionRepository = sessionRepository;
        this.stepRepository = stepRepository;
        this.timerRepository = timerRepository;
        this.reminderRepository = reminderRepository;
        this.techniqueCardRepository = techniqueCardRepository;
        this.timerStateMachine = timerStateMachine;
        this.checkpointService = checkpointService;
        this.clock = clock;
    }

    @Transactional
    public PracticeSessionResponse createSession(CreatePracticeSessionRequest request, User actor) {
        PracticeSession session = new PracticeSession();
        session.setUser(actor);
        session.setTitle(request.title());
        session.setStatus(SessionStatus.IN_PROGRESS);
        session = sessionRepository.save(session);

        Set<Long> cardIds = request.steps().stream()
                .flatMap(s -> s.techniqueCardIds() == null ? java.util.stream.Stream.empty() : s.techniqueCardIds().stream())
                .collect(Collectors.toSet());
        Map<Long, TechniqueCard> cardMap = techniqueCardRepository.findAllById(cardIds).stream()
                .collect(Collectors.toMap(TechniqueCard::getId, c -> c));

        List<PracticeStep> createdSteps = new ArrayList<>();
        for (int i = 0; i < request.steps().size(); i++) {
            CreatePracticeSessionRequest.StepInput input = request.steps().get(i);
            PracticeStep step = new PracticeStep();
            step.setSession(session);
            step.setStepOrder(i + 1);
            step.setName(input.name());
            step.setInstruction(input.instruction());
            step.setEstimatedSeconds(input.estimatedSeconds());
            step.setStatus(StepStatus.PENDING);
            step = stepRepository.save(step);

            if (input.techniqueCardIds() != null && !input.techniqueCardIds().isEmpty()) {
                Set<TechniqueCard> cards = input.techniqueCardIds().stream()
                        .map(cardId -> cardMap.get(cardId))
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toCollection(HashSet::new));
                step.getTechniqueCards().addAll(cards);
            }
            createdSteps.add(step);

            for (CreatePracticeSessionRequest.TimerInput timerInput : input.timers()) {
                StepTimer timer = new StepTimer();
                timer.setStep(step);
                timer.setTimerKey(timerInput.timerKey());
                timer.setDurationSeconds(timerInput.durationSeconds());
                timer.setRemainingSeconds(timerInput.durationSeconds());
                timer.setState(TimerState.NOT_STARTED);
                timer = timerRepository.save(timer);

                if (timerInput.reminders() != null) {
                    for (CreatePracticeSessionRequest.ReminderInput reminderInput : timerInput.reminders()) {
                        StepReminder reminder = new StepReminder();
                        reminder.setTimer(timer);
                        reminder.setOffsetSecondsBeforeDue(reminderInput.offsetSecondsBeforeDue());
                        reminder.setMessage(reminderInput.message());
                        reminder.setTriggered(false);
                        reminderRepository.save(reminder);
                    }
                }
            }
        }
        checkpointService.autoSaveForSession(session);
        return toSessionResponse(session, createdSteps, timerRepository.findByStepSessionId(session.getId()));
    }

    @Transactional
    public TimerStateResponse commandTimer(Long sessionId, Long timerId, TimerAction action, User actor) {
        PracticeSession session = loadOwnedSessionForUpdate(sessionId, actor);
        StepTimer timer = timerRepository.findByIdAndStepSessionId(timerId, sessionId)
                .orElseThrow(() -> new NotFoundException("Timer not found"));
        LocalDateTime now = LocalDateTime.now(clock);

        if (action == TimerAction.START) {
            timerStateMachine.start(timer, now);
        } else if (action == TimerAction.PAUSE) {
            timerStateMachine.pause(timer, now);
        } else if (action == TimerAction.RESUME) {
            timerStateMachine.resume(timer, now);
        }
        timerRepository.save(timer);
        updateReminderSchedule(timer);
        checkpointService.autoSaveForSession(session);
        return toTimerResponse(timer);
    }

    @Transactional
    public StepCompleteResponse completeStep(Long sessionId, Long stepId, User actor) {
        PracticeSession session = loadOwnedSessionForUpdate(sessionId, actor);
        PracticeStep step = stepRepository.findByIdAndSessionId(stepId, sessionId)
                .orElseThrow(() -> new NotFoundException("Step not found"));

        LocalDateTime now = LocalDateTime.now(clock);
        step.setStatus(StepStatus.COMPLETED);
        step.setCompletedAt(now);
        stepRepository.save(step);

        List<StepTimer> timers = timerRepository.findByStepId(step.getId());
        for (StepTimer timer : timers) {
            timer.setState(TimerState.COMPLETED);
            timer.setRemainingSeconds(0L);
            timer.setCompletedAt(now);
            timer.setDueAt(now);
            timerRepository.save(timer);
            updateReminderSchedule(timer);
        }

        List<PracticeStep> allSteps = stepRepository.findBySessionIdOrderByStepOrderAsc(sessionId);
        boolean allDone = allSteps.stream().allMatch(s -> s.getStatus() == StepStatus.COMPLETED);
        if (allDone) {
            session.setStatus(SessionStatus.COMPLETED);
            sessionRepository.save(session);
        }

        checkpointService.saveCheckpoint(sessionId, actor, CheckpointType.STEP_CHANGE);
        return new StepCompleteResponse(step.getId(), step.getStatus(), step.getCompletedAt());
    }

    @Transactional
    public PracticeSessionResponse getSession(Long sessionId, User actor) {
        PracticeSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Session not found"));
        if (!session.getUser().getId().equals(actor.getId())) {
            throw new NotFoundException("Session not found");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        List<StepTimer> runningTimers = timerRepository.findByStepSessionId(sessionId);
        boolean changed = false;
        for (StepTimer timer : runningTimers) {
            if (timer.getState() == TimerState.RUNNING) {
                changed = timerStateMachine.reconcileRunningTimer(timer, now) || changed;
            }
        }
        if (changed) {
            timerRepository.saveAll(runningTimers);
        }
        List<PracticeStep> steps = stepRepository.findBySessionIdOrderByStepOrderAsc(sessionId);
        List<StepTimer> timers = timerRepository.findByStepSessionId(sessionId);
        return toSessionResponse(session, steps, timers);
    }

    private void updateReminderSchedule(StepTimer timer) {
        List<StepReminder> reminders = reminderRepository.findByTimerId(timer.getId());
        LocalDateTime now = LocalDateTime.now(clock);
        for (StepReminder reminder : reminders) {
            if (timer.getState() == TimerState.RUNNING && timer.getDueAt() != null) {
                LocalDateTime remindAt = timer.getDueAt().minusSeconds(reminder.getOffsetSecondsBeforeDue());
                reminder.setRemindAt(remindAt);
                reminder.setTriggered(false);
                reminder.setTriggeredAt(null);
                if (remindAt.isBefore(now)) {
                    reminder.setRemindAt(now);
                }
            } else {
                reminder.setRemindAt(null);
                if (timer.getState() == TimerState.COMPLETED) {
                    reminder.setTriggered(true);
                    reminder.setTriggeredAt(now);
                }
            }
        }
        reminderRepository.saveAll(reminders);
    }

    private PracticeSession loadOwnedSessionForUpdate(Long sessionId, User actor) {
        PracticeSession session = sessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new NotFoundException("Session not found"));
        if (!session.getUser().getId().equals(actor.getId())) {
            throw new NotFoundException("Session not found");
        }
        return session;
    }

    private PracticeSessionResponse toSessionResponse(PracticeSession session, List<PracticeStep> steps, List<StepTimer> timers) {
        Map<Long, List<StepTimer>> timersByStep = timers.stream().collect(Collectors.groupingBy(t -> t.getStep().getId()));
        List<PracticeSessionResponse.StepView> stepViews = steps.stream().map(step -> {
            List<PracticeSessionResponse.TimerView> timerViews = timersByStep
                    .getOrDefault(step.getId(), List.of())
                    .stream()
                    .map(this::toTimerView)
                    .toList();
            return new PracticeSessionResponse.StepView(
                    step.getId(),
                    step.getStepOrder(),
                    step.getName(),
                    step.getInstruction(),
                    step.getStatus(),
                    timerViews
            );
        }).toList();
        return new PracticeSessionResponse(session.getId(), session.getTitle(), session.getStatus(), stepViews);
    }

    private PracticeSessionResponse.TimerView toTimerView(StepTimer timer) {
        return new PracticeSessionResponse.TimerView(
                timer.getId(),
                timer.getTimerKey(),
                timer.getState(),
                timer.getDurationSeconds(),
                timer.getRemainingSeconds(),
                timer.getDueAt()
        );
    }

    private TimerStateResponse toTimerResponse(StepTimer timer) {
        return new TimerStateResponse(timer.getId(), timer.getTimerKey(), timer.getState(), timer.getRemainingSeconds(), timer.getDueAt());
    }
}
