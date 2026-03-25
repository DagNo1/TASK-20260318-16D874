package com.pettrade.practiceplatform.service.timer;

import com.pettrade.practiceplatform.domain.StepTimer;
import com.pettrade.practiceplatform.domain.enumtype.TimerState;
import com.pettrade.practiceplatform.exception.BusinessRuleException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class TimerStateMachine {

    public void start(StepTimer timer, LocalDateTime now) {
        if (timer.getState() != TimerState.NOT_STARTED) {
            throw new BusinessRuleException("Timer can only be started from NOT_STARTED state");
        }
        timer.setState(TimerState.RUNNING);
        timer.setStartedAt(now);
        timer.setLastResumedAt(now);
        timer.setDueAt(now.plusSeconds(timer.getRemainingSeconds()));
    }

    public void pause(StepTimer timer, LocalDateTime now) {
        if (timer.getState() != TimerState.RUNNING) {
            throw new BusinessRuleException("Timer can only be paused from RUNNING state");
        }
        long remaining = calculateRemaining(timer, now);
        timer.setRemainingSeconds(remaining);
        timer.setState(remaining == 0 ? TimerState.COMPLETED : TimerState.PAUSED);
        timer.setLastPausedAt(now);
        timer.setDueAt(remaining == 0 ? now : null);
        if (remaining == 0) {
            timer.setCompletedAt(now);
        }
    }

    public void resume(StepTimer timer, LocalDateTime now) {
        if (timer.getState() != TimerState.PAUSED) {
            throw new BusinessRuleException("Timer can only be resumed from PAUSED state");
        }
        if (timer.getRemainingSeconds() <= 0) {
            timer.setRemainingSeconds(0L);
            timer.setState(TimerState.COMPLETED);
            timer.setCompletedAt(now);
            timer.setDueAt(now);
            return;
        }
        timer.setState(TimerState.RUNNING);
        timer.setLastResumedAt(now);
        timer.setDueAt(now.plusSeconds(timer.getRemainingSeconds()));
    }

    public boolean reconcileRunningTimer(StepTimer timer, LocalDateTime now) {
        if (timer.getState() != TimerState.RUNNING || timer.getDueAt() == null) {
            return false;
        }
        long remaining = calculateRemaining(timer, now);
        timer.setRemainingSeconds(remaining);
        if (remaining == 0) {
            timer.setState(TimerState.COMPLETED);
            timer.setCompletedAt(now);
            timer.setDueAt(now);
        }
        return true;
    }

    private long calculateRemaining(StepTimer timer, LocalDateTime now) {
        long seconds = java.time.Duration.between(now, timer.getDueAt()).getSeconds();
        return Math.max(0L, seconds);
    }
}
