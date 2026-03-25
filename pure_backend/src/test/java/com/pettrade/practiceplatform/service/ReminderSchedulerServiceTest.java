package com.pettrade.practiceplatform.service;

import com.pettrade.practiceplatform.domain.PracticeSession;
import com.pettrade.practiceplatform.domain.PracticeStep;
import com.pettrade.practiceplatform.domain.StepNodeReminder;
import com.pettrade.practiceplatform.domain.StepReminder;
import com.pettrade.practiceplatform.domain.StepTimer;
import com.pettrade.practiceplatform.repository.PracticeStepRepository;
import com.pettrade.practiceplatform.repository.StepNodeReminderRepository;
import com.pettrade.practiceplatform.repository.StepReminderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReminderSchedulerServiceTest {

    @Mock
    private StepReminderRepository stepReminderRepository;
    @Mock
    private StepNodeReminderRepository stepNodeReminderRepository;
    @Mock
    private PracticeStepRepository stepRepository;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private ReminderSchedulerService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-03-25T12:00:00Z"), ZoneOffset.UTC);
        service = new ReminderSchedulerService(stepReminderRepository, stepNodeReminderRepository, stepRepository, messagingTemplate, clock);
    }

    @Test
    void triggersBothTimerAndNodeRemindersIncludingRecoveredSchedules() {
        LocalDateTime now = LocalDateTime.of(2026, 3, 25, 12, 0, 0);

        PracticeSession session = new PracticeSession();
        ReflectionTestUtils.setField(session, "id", 9L);

        PracticeStep step = new PracticeStep();
        ReflectionTestUtils.setField(step, "id", 8L);
        step.setSession(session);
        step.setActiveElapsedSeconds(50L);
        step.setActiveSince(now.minusSeconds(10));

        StepTimer timer = new StepTimer();
        ReflectionTestUtils.setField(timer, "id", 7L);
        timer.setStep(step);
        timer.setTimerKey("boil");

        StepReminder dueTimerReminder = new StepReminder();
        dueTimerReminder.setTimer(timer);
        dueTimerReminder.setMessage("stir now");

        StepNodeReminder unscheduledNode = new StepNodeReminder();
        ReflectionTestUtils.setField(unscheduledNode, "id", 6L);
        unscheduledNode.setStep(step);
        unscheduledNode.setOffsetSecondsAfterStepStart(55L);
        unscheduledNode.setMessage("flip at checkpoint");
        unscheduledNode.setTriggered(false);

        when(stepNodeReminderRepository.findByTriggeredFalseAndRemindAtIsNullAndStepActiveSinceIsNotNull())
                .thenReturn(List.of(unscheduledNode));
        when(stepRepository.findById(8L)).thenReturn(Optional.of(step));
        when(stepReminderRepository.findByTriggeredFalseAndRemindAtBefore(now.plusSeconds(1)))
                .thenReturn(List.of(dueTimerReminder));
        when(stepNodeReminderRepository.findByTriggeredFalseAndRemindAtBefore(now.plusSeconds(1)))
                .thenReturn(List.of(unscheduledNode));

        service.triggerDueReminders();

        verify(messagingTemplate, atLeastOnce()).convertAndSend(org.mockito.ArgumentMatchers.eq("/topic/sessions/9/reminders"), org.mockito.ArgumentMatchers.any());
    }
}
