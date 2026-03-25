package com.pettrade.practiceplatform.repository;

import com.pettrade.practiceplatform.domain.ApprovalDecision;
import com.pettrade.practiceplatform.domain.enumtype.ApprovalDecisionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApprovalDecisionRepository extends JpaRepository<ApprovalDecision, Long> {
    Optional<ApprovalDecision> findByApprovalRequestIdAndAdminUserId(Long approvalRequestId, Long adminUserId);
    long countByApprovalRequestIdAndDecision(Long approvalRequestId, ApprovalDecisionType decision);
    List<ApprovalDecision> findByApprovalRequestId(Long approvalRequestId);
}
