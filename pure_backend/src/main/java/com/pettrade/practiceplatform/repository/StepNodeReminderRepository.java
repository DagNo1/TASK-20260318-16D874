package com.pettrade.practiceplatform.repository;

import com.pettrade.practiceplatform.domain.StepNodeReminder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface StepNodeReminderRepository extends JpaRepository<StepNodeReminder, Long> {
    List<StepNodeReminder> findByTriggeredFalseAndRemindAtBefore(LocalDateTime cutoff);
    List<StepNodeReminder> findByTriggeredFalseAndRemindAtIsNullAndStepActiveSinceIsNotNull();
    List<StepNodeReminder> findByStepId(Long stepId);
}
