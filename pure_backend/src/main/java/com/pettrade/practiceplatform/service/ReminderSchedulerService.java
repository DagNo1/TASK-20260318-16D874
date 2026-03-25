package com.pettrade.practiceplatform.service;

import com.pettrade.practiceplatform.domain.StepReminder;
import com.pettrade.practiceplatform.domain.StepNodeReminder;
import com.pettrade.practiceplatform.domain.PracticeStep;
import com.pettrade.practiceplatform.repository.StepNodeReminderRepository;
import com.pettrade.practiceplatform.repository.PracticeStepRepository;
import com.pettrade.practiceplatform.repository.StepReminderRepository;
import com.pettrade.practiceplatform.websocket.ReminderTriggeredEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ReminderSchedulerService {

    private final StepReminderRepository reminderRepository;
    private final StepNodeReminderRepository nodeReminderRepository;
    private final PracticeStepRepository stepRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final Clock clock;

    public ReminderSchedulerService(
            StepReminderRepository reminderRepository,
            StepNodeReminderRepository nodeReminderRepository,
            PracticeStepRepository stepRepository,
            SimpMessagingTemplate messagingTemplate,
            Clock clock
    ) {
        this.reminderRepository = reminderRepository;
        this.nodeReminderRepository = nodeReminderRepository;
        this.stepRepository = stepRepository;
        this.messagingTemplate = messagingTemplate;
        this.clock = clock;
    }

    @Transactional
    @Scheduled(fixedDelayString = "${app.scheduler.reminder-poll-ms:1000}")
    public void triggerDueReminders() {
        LocalDateTime now = LocalDateTime.now(clock);

        recoverMissedNodeSchedules(now);

        List<StepReminder> due = reminderRepository.findByTriggeredFalseAndRemindAtBefore(now.plusSeconds(1));
        for (StepReminder reminder : due) {
            reminder.setTriggered(true);
            reminder.setTriggeredAt(now);

            Long sessionId = reminder.getTimer().getStep().getSession().getId();
            messagingTemplate.convertAndSend(
                    "/topic/sessions/" + sessionId + "/reminders",
                    new ReminderTriggeredEvent(
                            "TIMER_REMINDER_TRIGGERED",
                            sessionId,
                            reminder.getTimer().getStep().getId(),
                            reminder.getTimer().getId(),
                            reminder.getTimer().getTimerKey(),
                            reminder.getMessage(),
                            now
                    )
            );
        }
        reminderRepository.saveAll(due);

        List<StepNodeReminder> nodeDue = nodeReminderRepository.findByTriggeredFalseAndRemindAtBefore(now.plusSeconds(1));
        for (StepNodeReminder reminder : nodeDue) {
            reminder.setTriggered(true);
            reminder.setTriggeredAt(now);

            Long sessionId = reminder.getStep().getSession().getId();
            messagingTemplate.convertAndSend(
                    "/topic/sessions/" + sessionId + "/reminders",
                    new ReminderTriggeredEvent(
                            "NODE_REMINDER_TRIGGERED",
                            sessionId,
                            reminder.getStep().getId(),
                            null,
                            null,
                            reminder.getMessage(),
                            now
                    )
            );
        }
        nodeReminderRepository.saveAll(nodeDue);
    }

    private void recoverMissedNodeSchedules(LocalDateTime now) {
        List<StepNodeReminder> unscheduled = nodeReminderRepository
                .findByTriggeredFalseAndRemindAtIsNullAndStepActiveSinceIsNotNull();
        for (StepNodeReminder reminder : unscheduled) {
            Optional<PracticeStep> stepOpt = stepRepository.findById(reminder.getStep().getId());
            if (stepOpt.isEmpty()) {
                continue;
            }
            PracticeStep step = stepOpt.get();
            if (step.getActiveSince() == null) {
                continue;
            }
            long elapsed = step.getActiveElapsedSeconds() + Math.max(0L, Duration.between(step.getActiveSince(), now).getSeconds());
            long remaining = reminder.getOffsetSecondsAfterStepStart() - elapsed;
            reminder.setRemindAt(remaining <= 0 ? now : now.plusSeconds(remaining));
        }
        nodeReminderRepository.saveAll(unscheduled);
    }
}
