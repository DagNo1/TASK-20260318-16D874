package com.pettrade.practiceplatform.repository;

import com.pettrade.practiceplatform.domain.SessionCheckpoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SessionCheckpointRepository extends JpaRepository<SessionCheckpoint, Long> {
    Optional<SessionCheckpoint> findTopBySessionIdOrderByCreatedAtDesc(Long sessionId);
}
