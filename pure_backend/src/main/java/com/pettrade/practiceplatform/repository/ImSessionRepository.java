package com.pettrade.practiceplatform.repository;

import com.pettrade.practiceplatform.domain.ImSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImSessionRepository extends JpaRepository<ImSession, Long> {
}
