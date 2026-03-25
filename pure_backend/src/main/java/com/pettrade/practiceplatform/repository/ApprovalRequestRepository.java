package com.pettrade.practiceplatform.repository;

import com.pettrade.practiceplatform.domain.ApprovalRequest;
import com.pettrade.practiceplatform.domain.enumtype.ApprovalRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {
    List<ApprovalRequest> findByStatusOrderByCreatedAtDesc(ApprovalRequestStatus status);
}
