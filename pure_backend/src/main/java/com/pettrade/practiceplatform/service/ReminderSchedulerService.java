package com.pettrade.practiceplatform.service;

import com.pettrade.practiceplatform.domain.StepReminder;
import com.pettrade.practiceplatform.repository.StepReminderRepository;
import com.pettrade.practiceplatform.websocket.ReminderTriggeredEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReminderSchedulerService {

    private final StepReminderRepository reminderRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final Clock clock;

    public ReminderSchedulerService(
            StepReminderRepository reminderRepository,
            SimpMessagingTemplate messagingTemplate,
            Clock clock
    ) {
        this.reminderRepository = reminderRepository;
        this.messagingTemplate = messagingTemplate;
        this.clock = clock;
    }

    @Transactional
    @Scheduled(fixedDelayString = "${app.scheduler.reminder-poll-ms:1000}")
    public void triggerDueReminders() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<StepReminder> due = reminderRepository.findByTriggeredFalseAndRemindAtBefore(now.plusSeconds(1));
        for (StepReminder reminder : due) {
            reminder.setTriggered(true);
            reminder.setTriggeredAt(now);

            Long sessionId = reminder.getTimer().getStep().getSession().getId();
            messagingTemplate.convertAndSend(
                    "/topic/sessions/" + sessionId + "/reminders",
                    new ReminderTriggeredEvent(
                            "REMINDER_TRIGGERED",
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
    }
}
