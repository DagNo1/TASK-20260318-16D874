package com.pettrade.practiceplatform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pettrade.practiceplatform.api.approval.ApprovalRequestCreateDto;
import com.pettrade.practiceplatform.api.approval.ApprovalRequestView;
import com.pettrade.practiceplatform.api.approval.AuditLogView;
import com.pettrade.practiceplatform.domain.ApprovalDecision;
import com.pettrade.practiceplatform.domain.ApprovalRequest;
import com.pettrade.practiceplatform.domain.AuditLog;
import com.pettrade.practiceplatform.domain.User;
import com.pettrade.practiceplatform.domain.enumtype.ApprovalDecisionType;
import com.pettrade.practiceplatform.domain.enumtype.ApprovalRequestStatus;
import com.pettrade.practiceplatform.domain.enumtype.ApprovalRequestType;
import com.pettrade.practiceplatform.exception.BusinessRuleException;
import com.pettrade.practiceplatform.exception.NotFoundException;
import com.pettrade.practiceplatform.repository.ApprovalDecisionRepository;
import com.pettrade.practiceplatform.repository.ApprovalRequestRepository;
import com.pettrade.practiceplatform.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class ApprovalService {

    private final ApprovalRequestRepository requestRepository;
    private final ApprovalDecisionRepository decisionRepository;
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public ApprovalService(
            ApprovalRequestRepository requestRepository,
            ApprovalDecisionRepository decisionRepository,
            AuditLogRepository auditLogRepository,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.requestRepository = requestRepository;
        this.decisionRepository = decisionRepository;
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public ApprovalRequestView createRequest(ApprovalRequestCreateDto dto, User actor) {
        ensurePlatformAdmin(actor);
        ApprovalRequest request = new ApprovalRequest();
        request.setRequestType(ApprovalRequestType.valueOf(dto.requestType().trim().toUpperCase()));
        request.setStatus(ApprovalRequestStatus.PENDING);
        request.setTargetResource(dto.targetResource());
        request.setTargetAction(dto.targetAction());
        request.setPayloadJson(dto.payloadJson());
        request.setRequestedByUser(actor);
        ApprovalRequest saved = requestRepository.save(request);

        appendAudit("APPROVAL_REQUEST_CREATED", actor, saved, Map.of("requestId", saved.getId()));
        return toView(saved);
    }

    @Transactional
    public ApprovalRequestView decide(Long requestId, ApprovalDecisionType decisionType, String comment, User actor) {
        ensurePlatformAdmin(actor);
        ApprovalRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Approval request not found"));

        if (request.getStatus() != ApprovalRequestStatus.PENDING) {
            throw new BusinessRuleException("Approval request is no longer pending");
        }
        if (request.getRequestedByUser().getId().equals(actor.getId())) {
            throw new BusinessRuleException("Requester cannot self-approve or self-reject");
        }
        if (decisionRepository.findByApprovalRequestIdAndAdminUserId(requestId, actor.getId()).isPresent()) {
            throw new BusinessRuleException("Admin already decided for this request");
        }

        ApprovalDecision decision = new ApprovalDecision();
        decision.setApprovalRequest(request);
        decision.setAdminUser(actor);
        decision.setDecision(decisionType);
        decision.setComment(comment);
        decisionRepository.save(decision);
        appendAudit("APPROVAL_DECISION_RECORDED", actor, request, Map.of("decision", decisionType.name()));

        LocalDateTime now = LocalDateTime.now(clock);
        if (decisionType == ApprovalDecisionType.REJECT) {
            request.setStatus(ApprovalRequestStatus.REJECTED);
            request.setDecidedAt(now);
            requestRepository.save(request);
            appendAudit("APPROVAL_REQUEST_REJECTED", actor, request, Map.of("requestId", request.getId()));
            return toView(request);
        }

        long approves = decisionRepository.countByApprovalRequestIdAndDecision(requestId, ApprovalDecisionType.APPROVE);
        if (approves >= 2) {
            request.setStatus(ApprovalRequestStatus.APPROVED);
            request.setDecidedAt(now);
            requestRepository.save(request);
            appendAudit("APPROVAL_REQUEST_APPROVED", actor, request, Map.of("requestId", request.getId()));
        }

        return toView(request);
    }

    @Transactional
    public ApprovalRequestView executeApproved(Long requestId, User actor) {
        ensurePlatformAdmin(actor);
        ApprovalRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Approval request not found"));
        if (request.getStatus() != ApprovalRequestStatus.APPROVED) {
            throw new BusinessRuleException("Cannot execute non-approved request");
        }
        if (request.getExecutedAt() != null) {
            throw new BusinessRuleException("Request already executed");
        }

        request.setExecutedAt(LocalDateTime.now(clock));
        requestRepository.save(request);
        appendAudit(
                "PROTECTED_OPERATION_EXECUTED",
                actor,
                request,
                Map.of("targetResource", request.getTargetResource(), "targetAction", request.getTargetAction())
        );
        return toView(request);
    }

    @Transactional(readOnly = true)
    public List<ApprovalRequestView> listPending() {
        return requestRepository.findByStatusOrderByCreatedAtDesc(ApprovalRequestStatus.PENDING).stream()
                .map(this::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditLogView> listAuditForRequest(Long requestId) {
        return auditLogRepository.findByApprovalRequestIdOrderByIdAsc(requestId)
                .stream()
                .map(a -> new AuditLogView(a.getId(), a.getEventType(), a.getActorUser().getId(), a.getDetailsJson()))
                .toList();
    }

    private void appendAudit(String eventType, User actor, ApprovalRequest request, Map<String, Object> details) {
        AuditLog log = new AuditLog();
        log.setEventType(eventType);
        log.setActorUser(actor);
        log.setApprovalRequest(request);
        log.setDetailsJson(writeJson(details));
        auditLogRepository.save(log);
    }

    private String writeJson(Map<String, Object> details) {
        try {
            return objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize audit details", e);
        }
    }

    private ApprovalRequestView toView(ApprovalRequest r) {
        return new ApprovalRequestView(
                r.getId(),
                r.getRequestType().name(),
                r.getStatus().name(),
                r.getTargetResource(),
                r.getTargetAction(),
                r.getPayloadJson(),
                r.getRequestedByUser().getId(),
                r.getCreatedAt(),
                r.getDecidedAt(),
                r.getExecutedAt()
        );
    }

    private void ensurePlatformAdmin(User user) {
        boolean admin = user.getRoles().stream().anyMatch(r -> "ROLE_PLATFORM_ADMIN".equals(r.getName()));
        if (!admin) {
            throw new BusinessRuleException("Platform admin role required");
        }
    }
}
