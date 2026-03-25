package com.pettrade.practiceplatform.repository;

import com.pettrade.practiceplatform.domain.PracticeStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PracticeStepRepository extends JpaRepository<PracticeStep, Long> {
    List<PracticeStep> findBySessionIdOrderByStepOrderAsc(Long sessionId);
    java.util.Optional<PracticeStep> findByIdAndSessionId(Long stepId, Long sessionId);
}
