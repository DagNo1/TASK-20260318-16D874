package com.pettrade.practiceplatform.api.approval;

import com.pettrade.practiceplatform.domain.User;
import com.pettrade.practiceplatform.domain.enumtype.ApprovalDecisionType;
import com.pettrade.practiceplatform.service.ApprovalService;
import com.pettrade.practiceplatform.service.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/approvals")
@Tag(name = "Approvals")
public class ApprovalController {

    private final ApprovalService approvalService;
    private final CurrentUserService currentUserService;

    public ApprovalController(ApprovalService approvalService, CurrentUserService currentUserService) {
        this.approvalService = approvalService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Create approval request")
    public ResponseEntity<ApprovalRequestView> create(@Valid @RequestBody ApprovalRequestCreateDto dto) {
        User user = currentUserService.currentUser();
        return ResponseEntity.ok(approvalService.createRequest(dto, user));
    }

    @PostMapping("/{requestId}/approve")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Approve request (2 distinct admins required)")
    public ResponseEntity<ApprovalRequestView> approve(@PathVariable Long requestId, @RequestBody(required = false) ApprovalDecisionDto dto) {
        User user = currentUserService.currentUser();
        String comment = dto == null ? null : dto.comment();
        return ResponseEntity.ok(approvalService.decide(requestId, ApprovalDecisionType.APPROVE, comment, user));
    }

    @PostMapping("/{requestId}/reject")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Reject request")
    public ResponseEntity<ApprovalRequestView> reject(@PathVariable Long requestId, @RequestBody(required = false) ApprovalDecisionDto dto) {
        User user = currentUserService.currentUser();
        String comment = dto == null ? null : dto.comment();
        return ResponseEntity.ok(approvalService.decide(requestId, ApprovalDecisionType.REJECT, comment, user));
    }

    @PostMapping("/{requestId}/execute")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Execute approved protected operation")
    public ResponseEntity<ApprovalRequestView> execute(@PathVariable Long requestId) {
        User user = currentUserService.currentUser();
        return ResponseEntity.ok(approvalService.executeApproved(requestId, user));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "List pending approval requests")
    public ResponseEntity<List<ApprovalRequestView>> pending() {
        return ResponseEntity.ok(approvalService.listPending());
    }

    @GetMapping("/{requestId}/audit")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "List immutable audit logs for request")
    public ResponseEntity<List<AuditLogView>> audit(@PathVariable Long requestId) {
        return ResponseEntity.ok(approvalService.listAuditForRequest(requestId));
    }
}
