package com.pettrade.practiceplatform.service;

import com.pettrade.practiceplatform.domain.PracticeStep;
import com.pettrade.practiceplatform.domain.StepNodeReminder;
import com.pettrade.practiceplatform.domain.StepReminder;
import com.pettrade.practiceplatform.domain.StepTimer;
import com.pettrade.practiceplatform.domain.enumtype.StepStatus;
import com.pettrade.practiceplatform.domain.enumtype.TimerState;
import com.pettrade.practiceplatform.repository.PracticeStepRepository;
import com.pettrade.practiceplatform.repository.StepNodeReminderRepository;
import com.pettrade.practiceplatform.repository.StepReminderRepository;
import com.pettrade.practiceplatform.repository.StepTimerRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReminderSchedulingService {

    private final StepReminderRepository stepReminderRepository;
    private final StepNodeReminderRepository stepNodeReminderRepository;
    private final StepTimerRepository stepTimerRepository;
    private final PracticeStepRepository practiceStepRepository;

    public ReminderSchedulingService(
            StepReminderRepository stepReminderRepository,
            StepNodeReminderRepository stepNodeReminderRepository,
            StepTimerRepository stepTimerRepository,
            PracticeStepRepository practiceStepRepository
    ) {
        this.stepReminderRepository = stepReminderRepository;
        this.stepNodeReminderRepository = stepNodeReminderRepository;
        this.stepTimerRepository = stepTimerRepository;
        this.practiceStepRepository = practiceStepRepository;
    }

    public void rescheduleTimerReminders(StepTimer timer, LocalDateTime now) {
        List<StepReminder> reminders = stepReminderRepository.findByTimerId(timer.getId());
        for (StepReminder reminder : reminders) {
            if (timer.getState() == TimerState.RUNNING && timer.getDueAt() != null) {
                LocalDateTime remindAt = timer.getDueAt().minusSeconds(reminder.getOffsetSecondsBeforeDue());
                reminder.setTriggered(false);
                reminder.setTriggeredAt(null);
                reminder.setRemindAt(remindAt.isBefore(now) ? now : remindAt);
            } else {
                reminder.setRemindAt(null);
                if (timer.getState() == TimerState.COMPLETED) {
                    reminder.setTriggered(true);
                    reminder.setTriggeredAt(now);
                }
            }
        }
        stepReminderRepository.saveAll(reminders);
    }

    public void updateStepActivityAndNodeReminders(PracticeStep step, LocalDateTime now) {
        List<StepTimer> timers = stepTimerRepository.findByStepId(step.getId());
        boolean hasRunningTimer = timers.stream().anyMatch(t -> t.getState() == TimerState.RUNNING);

        if (hasRunningTimer) {
            if (step.getStatus() == StepStatus.PENDING) {
                step.setStatus(StepStatus.IN_PROGRESS);
            }
            if (step.getActiveSince() == null) {
                step.setActiveSince(now);
            }
        } else if (step.getActiveSince() != null) {
            long elapsed = Math.max(0L, Duration.between(step.getActiveSince(), now).getSeconds());
            step.setActiveElapsedSeconds(step.getActiveElapsedSeconds() + elapsed);
            step.setActiveSince(null);
        }

        practiceStepRepository.save(step);
        rescheduleNodeReminders(step, now);
    }

    public void rescheduleNodeReminders(PracticeStep step, LocalDateTime now) {
        List<StepNodeReminder> nodeReminders = stepNodeReminderRepository.findByStepId(step.getId());

        if (step.getStatus() == StepStatus.COMPLETED) {
            for (StepNodeReminder reminder : nodeReminders) {
                reminder.setRemindAt(null);
                reminder.setTriggered(true);
                reminder.setTriggeredAt(now);
            }
            stepNodeReminderRepository.saveAll(nodeReminders);
            return;
        }

        boolean active = step.getActiveSince() != null;
        long elapsed = step.getActiveElapsedSeconds();
        if (active) {
            elapsed += Math.max(0L, Duration.between(step.getActiveSince(), now).getSeconds());
        }

        for (StepNodeReminder reminder : nodeReminders) {
            if (reminder.isTriggered()) {
                continue;
            }
            if (!active) {
                reminder.setRemindAt(null);
                continue;
            }
            long remaining = reminder.getOffsetSecondsAfterStepStart() - elapsed;
            reminder.setRemindAt(remaining <= 0 ? now : now.plusSeconds(remaining));
        }
        stepNodeReminderRepository.saveAll(nodeReminders);
    }

    public void rescheduleForSession(Long sessionId, LocalDateTime now) {
        List<PracticeStep> steps = practiceStepRepository.findBySessionIdOrderByStepOrderAsc(sessionId);
        for (PracticeStep step : steps) {
            List<StepTimer> timers = stepTimerRepository.findByStepId(step.getId());
            for (StepTimer timer : timers) {
                rescheduleTimerReminders(timer, now);
            }
            rescheduleNodeReminders(step, now);
        }
    }
}
