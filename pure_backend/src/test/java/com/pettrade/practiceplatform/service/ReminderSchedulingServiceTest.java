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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReminderSchedulingServiceTest {

    @Mock
    private StepReminderRepository stepReminderRepository;
    @Mock
    private StepNodeReminderRepository stepNodeReminderRepository;
    @Mock
    private StepTimerRepository stepTimerRepository;
    @Mock
    private PracticeStepRepository practiceStepRepository;

    private ReminderSchedulingService service;

    @BeforeEach
    void setUp() {
        service = new ReminderSchedulingService(
                stepReminderRepository,
                stepNodeReminderRepository,
                stepTimerRepository,
                practiceStepRepository
        );
    }

    @Test
    void pauseClearsNodeReminderScheduleAndResumeRecomputes() {
        LocalDateTime now = LocalDateTime.of(2026, 3, 25, 12, 0, 0);

        PracticeStep step = new PracticeStep();
        org.springframework.test.util.ReflectionTestUtils.setField(step, "id", 10L);
        step.setStatus(StepStatus.IN_PROGRESS);
        step.setActiveElapsedSeconds(20L);
        step.setActiveSince(null);

        StepNodeReminder node = new StepNodeReminder();
        node.setStep(step);
        node.setOffsetSecondsAfterStepStart(60L);
        node.setTriggered(false);

        when(stepNodeReminderRepository.findByStepId(10L)).thenReturn(List.of(node));
        service.rescheduleNodeReminders(step, now);
        assertNull(node.getRemindAt());

        step.setActiveSince(now);
        when(stepNodeReminderRepository.findByStepId(10L)).thenReturn(List.of(node));
        service.rescheduleNodeReminders(step, now.plusSeconds(5));
        assertEquals(now.plusSeconds(35), node.getRemindAt());
    }

    @Test
    void checkpointRestoreReschedulesTimerAndNodeReminders() {
        LocalDateTime now = LocalDateTime.of(2026, 3, 25, 12, 0, 0);

        PracticeStep step = new PracticeStep();
        org.springframework.test.util.ReflectionTestUtils.setField(step, "id", 21L);
        step.setStatus(StepStatus.IN_PROGRESS);
        step.setActiveElapsedSeconds(30L);
        step.setActiveSince(now.minusSeconds(10));

        StepTimer timer = new StepTimer();
        org.springframework.test.util.ReflectionTestUtils.setField(timer, "id", 31L);
        timer.setStep(step);
        timer.setState(TimerState.RUNNING);
        timer.setDueAt(now.plusSeconds(90));

        StepReminder timerReminder = new StepReminder();
        timerReminder.setTimer(timer);
        timerReminder.setOffsetSecondsBeforeDue(20L);
        timerReminder.setTriggered(false);

        StepNodeReminder nodeReminder = new StepNodeReminder();
        nodeReminder.setStep(step);
        nodeReminder.setOffsetSecondsAfterStepStart(120L);
        nodeReminder.setTriggered(false);

        when(practiceStepRepository.findBySessionIdOrderByStepOrderAsc(1L)).thenReturn(List.of(step));
        when(stepTimerRepository.findByStepId(21L)).thenReturn(List.of(timer));
        when(stepReminderRepository.findByTimerId(31L)).thenReturn(List.of(timerReminder));
        when(stepNodeReminderRepository.findByStepId(21L)).thenReturn(List.of(nodeReminder));

        service.rescheduleForSession(1L, now);

        assertEquals(now.plusSeconds(70), timerReminder.getRemindAt());
        assertEquals(now.plusSeconds(80), nodeReminder.getRemindAt());
    }

    @Test
    void timerReminderPauseAndResumeRecomputesDueSchedule() {
        LocalDateTime now = LocalDateTime.of(2026, 3, 25, 12, 0, 0);

        PracticeStep step = new PracticeStep();
        org.springframework.test.util.ReflectionTestUtils.setField(step, "id", 22L);

        StepTimer timer = new StepTimer();
        org.springframework.test.util.ReflectionTestUtils.setField(timer, "id", 32L);
        timer.setStep(step);
        timer.setState(TimerState.PAUSED);
        timer.setDueAt(null);

        StepReminder reminder = new StepReminder();
        reminder.setTimer(timer);
        reminder.setOffsetSecondsBeforeDue(15L);
        reminder.setTriggered(false);

        when(stepReminderRepository.findByTimerId(32L)).thenReturn(List.of(reminder));
        service.rescheduleTimerReminders(timer, now);
        assertNull(reminder.getRemindAt());

        timer.setState(TimerState.RUNNING);
        timer.setDueAt(now.plusSeconds(40));
        service.rescheduleTimerReminders(timer, now.plusSeconds(5));
        assertEquals(now.plusSeconds(25), reminder.getRemindAt());
    }
}
