package com.pettrade.practiceplatform.repository;

import com.pettrade.practiceplatform.domain.PracticeSession;
import com.pettrade.practiceplatform.domain.enumtype.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface PracticeSessionRepository extends JpaRepository<PracticeSession, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from PracticeSession s where s.id = :id")
    Optional<PracticeSession> findByIdForUpdate(@Param("id") Long id);

    List<PracticeSession> findByStatus(SessionStatus status);
}
