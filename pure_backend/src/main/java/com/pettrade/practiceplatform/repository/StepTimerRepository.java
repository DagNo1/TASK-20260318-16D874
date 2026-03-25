package com.pettrade.practiceplatform.repository;

import com.pettrade.practiceplatform.domain.StepTimer;
import com.pettrade.practiceplatform.domain.enumtype.TimerState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StepTimerRepository extends JpaRepository<StepTimer, Long> {
    Optional<StepTimer> findByStepIdAndTimerKey(Long stepId, String timerKey);
    Optional<StepTimer> findByIdAndStepSessionId(Long id, Long sessionId);
    List<StepTimer> findByStepId(Long stepId);
    List<StepTimer> findByStepSessionId(Long sessionId);
    List<StepTimer> findByStateAndDueAtBefore(TimerState state, LocalDateTime cutoff);
    List<StepTimer> findByIdIn(Collection<Long> ids);
}
