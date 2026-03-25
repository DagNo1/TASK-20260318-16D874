package com.pettrade.practiceplatform.service.timer;

import com.pettrade.practiceplatform.domain.StepTimer;
import com.pettrade.practiceplatform.domain.enumtype.TimerState;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimerStateMachineTest {

    private final TimerStateMachine timerStateMachine = new TimerStateMachine();

    @Test
    void startPauseResumeChangesStateAndRemainingTime() {
        StepTimer timer = new StepTimer();
        timer.setState(TimerState.NOT_STARTED);
        timer.setDurationSeconds(120L);
        timer.setRemainingSeconds(120L);

        LocalDateTime t0 = LocalDateTime.of(2026, 3, 25, 12, 0, 0);
        timerStateMachine.start(timer, t0);
        assertEquals(TimerState.RUNNING, timer.getState());
        assertEquals(t0.plusSeconds(120), timer.getDueAt());

        LocalDateTime t1 = t0.plusSeconds(30);
        timerStateMachine.pause(timer, t1);
        assertEquals(TimerState.PAUSED, timer.getState());
        assertEquals(90L, timer.getRemainingSeconds());

        LocalDateTime t2 = t1.plusSeconds(10);
        timerStateMachine.resume(timer, t2);
        assertEquals(TimerState.RUNNING, timer.getState());
        assertEquals(t2.plusSeconds(90), timer.getDueAt());
    }

    @Test
    void reconcileSupportsParallelTimersIndependently() {
        StepTimer timerA = new StepTimer();
        timerA.setState(TimerState.NOT_STARTED);
        timerA.setDurationSeconds(20L);
        timerA.setRemainingSeconds(20L);

        StepTimer timerB = new StepTimer();
        timerB.setState(TimerState.NOT_STARTED);
        timerB.setDurationSeconds(50L);
        timerB.setRemainingSeconds(50L);

        LocalDateTime start = LocalDateTime.of(2026, 3, 25, 12, 0, 0);
        timerStateMachine.start(timerA, start);
        timerStateMachine.start(timerB, start);

        LocalDateTime checkAt = start.plusSeconds(25);
        timerStateMachine.reconcileRunningTimer(timerA, checkAt);
        timerStateMachine.reconcileRunningTimer(timerB, checkAt);

        assertEquals(TimerState.COMPLETED, timerA.getState());
        assertEquals(0L, timerA.getRemainingSeconds());
        assertEquals(TimerState.RUNNING, timerB.getState());
        assertEquals(25L, timerB.getRemainingSeconds());
    }
}
