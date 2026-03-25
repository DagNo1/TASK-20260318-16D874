package com.pettrade.practiceplatform.repository;

import com.pettrade.practiceplatform.domain.PracticeAchievement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PracticeAchievementRepository extends JpaRepository<PracticeAchievement, Long> {
    Optional<PracticeAchievement> findByIdAndOwnerUserId(Long id, Long ownerUserId);
    List<PracticeAchievement> findByOwnerUserIdOrderByUpdatedAtDesc(Long ownerUserId);
}
