package com.pettrade.practiceplatform.repository;

import com.pettrade.practiceplatform.domain.StepReminder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface StepReminderRepository extends JpaRepository<StepReminder, Long> {
    List<StepReminder> findByTriggeredFalseAndRemindAtBefore(LocalDateTime cutoff);
    List<StepReminder> findByTimerId(Long timerId);
}
