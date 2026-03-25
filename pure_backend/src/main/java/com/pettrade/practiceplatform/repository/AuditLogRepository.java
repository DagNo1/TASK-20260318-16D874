package com.pettrade.practiceplatform.repository;

import com.pettrade.practiceplatform.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByApprovalRequestIdOrderByIdAsc(Long approvalRequestId);
}
