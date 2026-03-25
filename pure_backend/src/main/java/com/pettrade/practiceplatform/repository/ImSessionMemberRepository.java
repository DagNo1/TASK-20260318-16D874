package com.pettrade.practiceplatform.repository;

import com.pettrade.practiceplatform.domain.ImSessionMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ImSessionMemberRepository extends JpaRepository<ImSessionMember, Long> {
    Optional<ImSessionMember> findBySessionIdAndUserId(Long sessionId, Long userId);
    List<ImSessionMember> findBySessionId(Long sessionId);
}
