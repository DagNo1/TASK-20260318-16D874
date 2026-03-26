package com.pettrade.practiceplatform.repository;

import com.pettrade.practiceplatform.domain.ImMessage;
import com.pettrade.practiceplatform.domain.enumtype.ImMessageStatus;
import com.pettrade.practiceplatform.domain.enumtype.ImMessageType;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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

    @Modifying
    @Query("""
            update ImMessage m
            set m.status = :archivedStatus,
                m.archivedAt = :archivedAt
            where m.status = :sentStatus
              and m.createdAt < :cutoff
            """)
    int archiveOldSentMessages(
            @Param("sentStatus") ImMessageStatus sentStatus,
            @Param("archivedStatus") ImMessageStatus archivedStatus,
            @Param("archivedAt") LocalDateTime archivedAt,
            @Param("cutoff") LocalDateTime cutoff
    );
}
