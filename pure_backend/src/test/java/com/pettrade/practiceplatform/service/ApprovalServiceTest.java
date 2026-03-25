package com.pettrade.practiceplatform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pettrade.practiceplatform.api.approval.ApprovalRequestCreateDto;
import com.pettrade.practiceplatform.domain.ApprovalRequest;
import com.pettrade.practiceplatform.domain.Role;
import com.pettrade.practiceplatform.domain.User;
import com.pettrade.practiceplatform.domain.enumtype.ApprovalDecisionType;
import com.pettrade.practiceplatform.domain.enumtype.ApprovalRequestStatus;
import com.pettrade.practiceplatform.domain.enumtype.ApprovalRequestType;
import com.pettrade.practiceplatform.exception.BusinessRuleException;
import com.pettrade.practiceplatform.repository.ApprovalDecisionRepository;
import com.pettrade.practiceplatform.repository.ApprovalRequestRepository;
import com.pettrade.practiceplatform.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovalServiceTest {

    @Mock
    private ApprovalRequestRepository requestRepository;
    @Mock
    private ApprovalDecisionRepository decisionRepository;
    @Mock
    private AuditLogRepository auditLogRepository;

    private ApprovalService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-03-25T12:00:00Z"), ZoneOffset.UTC);
        service = new ApprovalService(
                requestRepository,
                decisionRepository,
                auditLogRepository,
                new ObjectMapper(),
                clock
        );
    }

    @Test
    void requesterCannotSelfApprove() {
        User requester = adminUser(1L, "requester");
        ApprovalRequest request = request(10L, requester, ApprovalRequestStatus.PENDING);

        when(requestRepository.findById(10L)).thenReturn(Optional.of(request));

        assertThrows(BusinessRuleException.class, () ->
                service.decide(10L, ApprovalDecisionType.APPROVE, "self", requester)
        );
    }

    @Test
    void sameAdminCannotApproveTwice() {
        User requester = adminUser(1L, "req");
        User adminA = adminUser(2L, "a");
        ApprovalRequest request = request(11L, requester, ApprovalRequestStatus.PENDING);

        when(requestRepository.findById(11L)).thenReturn(Optional.of(request));
        when(decisionRepository.findByApprovalRequestIdAndAdminUserId(11L, 2L)).thenReturn(Optional.of(new com.pettrade.practiceplatform.domain.ApprovalDecision()));

        assertThrows(BusinessRuleException.class, () ->
                service.decide(11L, ApprovalDecisionType.APPROVE, null, adminA)
        );
    }

    @Test
    void requiresTwoDistinctAdminsBeforeExecuteAndAuditsExecution() {
        User requester = adminUser(1L, "req");
        User adminA = adminUser(2L, "a");
        User adminB = adminUser(3L, "b");

        ApprovalRequest pending = request(20L, requester, ApprovalRequestStatus.PENDING);
        ApprovalRequest approved = request(20L, requester, ApprovalRequestStatus.APPROVED);
        ReflectionTestUtils.setField(approved, "decidedAt", LocalDateTime.of(2026, 3, 25, 12, 0, 0));

        when(requestRepository.findById(20L)).thenReturn(Optional.of(pending), Optional.of(pending), Optional.of(approved));
        when(decisionRepository.findByApprovalRequestIdAndAdminUserId(20L, 2L)).thenReturn(Optional.empty());
        when(decisionRepository.findByApprovalRequestIdAndAdminUserId(20L, 3L)).thenReturn(Optional.empty());
        when(decisionRepository.countByApprovalRequestIdAndDecision(20L, ApprovalDecisionType.APPROVE)).thenReturn(1L, 2L);
        when(requestRepository.save(any(ApprovalRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        service.decide(20L, ApprovalDecisionType.APPROVE, null, adminA);
        var afterSecond = service.decide(20L, ApprovalDecisionType.APPROVE, null, adminB);
        assertEquals("APPROVED", afterSecond.status());

        var executed = service.executeApproved(20L, adminB);
        assertEquals("APPROVED", executed.status());

        ArgumentCaptor<com.pettrade.practiceplatform.domain.AuditLog> captor = ArgumentCaptor.forClass(com.pettrade.practiceplatform.domain.AuditLog.class);
        verify(auditLogRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        boolean hasProtectedExecution = captor.getAllValues().stream()
                .anyMatch(a -> "PROTECTED_OPERATION_EXECUTED".equals(a.getEventType()));
        assertEquals(true, hasProtectedExecution);
    }

    @Test
    void cannotExecutePendingRequestBypass() {
        User requester = adminUser(1L, "req");
        User adminA = adminUser(2L, "a");
        ApprovalRequest pending = request(30L, requester, ApprovalRequestStatus.PENDING);
        when(requestRepository.findById(30L)).thenReturn(Optional.of(pending));

        assertThrows(BusinessRuleException.class, () -> service.executeApproved(30L, adminA));
    }

    @Test
    void createRequiresPlatformAdmin() {
        User nonAdmin = nonAdminUser(9L, "na");
        ApprovalRequestCreateDto dto = new ApprovalRequestCreateDto("PERMISSION_CHANGE", "USER_ROLE", "GRANT", "{}");
        assertThrows(BusinessRuleException.class, () -> service.createRequest(dto, nonAdmin));
    }

    private ApprovalRequest request(Long id, User requester, ApprovalRequestStatus status) {
        ApprovalRequest r = new ApprovalRequest();
        ReflectionTestUtils.setField(r, "id", id);
        r.setRequestType(ApprovalRequestType.PERMISSION_CHANGE);
        r.setStatus(status);
        r.setTargetResource("USER_ROLE");
        r.setTargetAction("GRANT");
        r.setPayloadJson("{}");
        r.setRequestedByUser(requester);
        ReflectionTestUtils.setField(r, "createdAt", LocalDateTime.of(2026, 3, 25, 11, 0, 0));
        return r;
    }

    private User adminUser(Long id, String username) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", id);
        user.setUsername(username);
        Role role = new Role();
        ReflectionTestUtils.setField(role, "name", "ROLE_PLATFORM_ADMIN");
        user.getRoles().add(role);
        return user;
    }

    private User nonAdminUser(Long id, String username) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", id);
        user.setUsername(username);
        return user;
    }
}
