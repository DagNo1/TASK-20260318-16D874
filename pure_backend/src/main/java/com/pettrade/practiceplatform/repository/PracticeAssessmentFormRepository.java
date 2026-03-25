package com.pettrade.practiceplatform.repository;

import com.pettrade.practiceplatform.domain.PracticeAssessmentForm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PracticeAssessmentFormRepository extends JpaRepository<PracticeAssessmentForm, Long> {
    Optional<PracticeAssessmentForm> findByAchievementId(Long achievementId);
}
