package com.pettrade.practiceplatform.repository;

import com.pettrade.practiceplatform.domain.PracticeAchievementAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PracticeAchievementAttachmentRepository extends JpaRepository<PracticeAchievementAttachment, Long> {
    List<PracticeAchievementAttachment> findByAchievementIdOrderByAttachmentVersionDesc(Long achievementId);
    Optional<PracticeAchievementAttachment> findTopByAchievementIdOrderByAttachmentVersionDesc(Long achievementId);
}
