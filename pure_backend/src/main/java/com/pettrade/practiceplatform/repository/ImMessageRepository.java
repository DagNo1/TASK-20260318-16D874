package com.pettrade.practiceplatform.repository;

import com.pettrade.practiceplatform.domain.ImMessage;
import com.pettrade.practiceplatform.domain.enumtype.ImMessageStatus;
import com.pettrade.practiceplatform.domain.enumtype.ImMessageType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ImMessageRepository extends JpaRepository<ImMessage, Long> {
    Optional<ImMessage> findTopBySessionIdAndSenderIdAndDedupKeyAndMessageTypeOrderByCreatedAtDesc(
            Long sessionId,
            Long senderId,
            String dedupKey,
            ImMessageType messageType
    );
    Optional<ImMessage> findByIdAndSessionId(Long id, Long sessionId);
    long countBySessionIdAndStatus(Long sessionId, ImMessageStatus status);
    long countBySessionIdAndIdGreaterThanAndStatus(Long sessionId, Long messageId, ImMessageStatus status);
}
