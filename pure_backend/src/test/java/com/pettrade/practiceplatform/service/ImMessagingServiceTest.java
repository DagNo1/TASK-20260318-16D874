package com.pettrade.practiceplatform.service;

import com.pettrade.practiceplatform.api.im.RecallMessageRequest;
import com.pettrade.practiceplatform.api.im.SendMessageRequest;
import com.pettrade.practiceplatform.domain.ImSessionMember;
import com.pettrade.practiceplatform.domain.ImMessage;
import com.pettrade.practiceplatform.domain.ImSession;
import com.pettrade.practiceplatform.domain.Role;
import com.pettrade.practiceplatform.domain.User;
import com.pettrade.practiceplatform.domain.enumtype.ImMessageStatus;
import com.pettrade.practiceplatform.domain.enumtype.ImMessageType;
import com.pettrade.practiceplatform.exception.BusinessRuleException;
import com.pettrade.practiceplatform.repository.ImImageAssetRepository;
import com.pettrade.practiceplatform.repository.ImMessageRepository;
import com.pettrade.practiceplatform.repository.ImSessionMemberRepository;
import com.pettrade.practiceplatform.repository.ImSessionRepository;
import com.pettrade.practiceplatform.service.im.ImSessionGuardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImMessagingServiceTest {

    @Mock
    private ImSessionRepository sessionRepository;
    @Mock
    private ImSessionMemberRepository sessionMemberRepository;
    @Mock
    private ImMessageRepository messageRepository;
    @Mock
    private ImImageAssetRepository imageAssetRepository;
    @Mock
    private ImSessionGuardService sessionGuardService;
    @Mock
    private NotificationService notificationService;

    private ImMessagingService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-03-25T12:00:00Z"), ZoneOffset.UTC);
        service = new ImMessagingService(
                sessionRepository,
                sessionMemberRepository,
                messageRepository,
                imageAssetRepository,
                sessionGuardService,
                notificationService,
                clock
        );
    }

    @Test
    void duplicateTextWithinWindowFoldsIntoSameMessage() {
        User sender = user(10L, "alice", false);
        ImSession session = session(100L, sender);
        doNothing().when(sessionGuardService).ensureMember(100L, sender);
        when(sessionRepository.findById(100L)).thenReturn(Optional.of(session));

        ImMessage existing = new ImMessage();
        ReflectionTestUtils.setField(existing, "id", 500L);
        existing.setSession(session);
        existing.setSender(sender);
        existing.setMessageType(ImMessageType.TEXT);
        existing.setStatus(ImMessageStatus.SENT);
        existing.setFoldedCount(1);
        ReflectionTestUtils.setField(existing, "createdAt", LocalDateTime.of(2026, 3, 25, 11, 59, 55));

        when(messageRepository.findTopBySessionIdAndSenderIdAndDedupKeyAndMessageTypeOrderByCreatedAtDesc(
                anyLong(), anyLong(), anyString(), any(ImMessageType.class)
        )).thenReturn(Optional.of(existing));
        when(messageRepository.save(existing)).thenReturn(existing);

        SendMessageRequest req = new SendMessageRequest("TEXT", "Hello there", null, null, null, null, "c1");
        var event = service.sendMessage(100L, req, sender);

        assertEquals("IM_MESSAGE_FOLDED", event.eventType());
        assertEquals(2, existing.getFoldedCount());
        assertEquals(500L, event.messageId());
    }

    @Test
    void nonOwnerNonAdminCannotRecallMessage() {
        User owner = user(1L, "owner", false);
        User other = user(2L, "other", false);
        ImSession session = session(77L, owner);
        ImMessage msg = new ImMessage();
        ReflectionTestUtils.setField(msg, "id", 888L);
        msg.setSession(session);
        msg.setSender(owner);
        msg.setMessageType(ImMessageType.TEXT);
        msg.setStatus(ImMessageStatus.SENT);
        msg.setFoldedCount(1);

        doNothing().when(sessionGuardService).ensureMember(77L, other);
        when(messageRepository.findByIdAndSessionId(888L, 77L)).thenReturn(Optional.of(msg));

        assertThrows(BusinessRuleException.class, () ->
                service.recallMessage(77L, new RecallMessageRequest(888L), other)
        );
    }

    @Test
    void adminCanRecallOthersMessage() {
        User owner = user(1L, "owner", false);
        User admin = user(2L, "admin", true);
        ImSession session = session(77L, owner);
        ImMessage msg = new ImMessage();
        ReflectionTestUtils.setField(msg, "id", 889L);
        msg.setSession(session);
        msg.setSender(owner);
        msg.setMessageType(ImMessageType.TEXT);
        msg.setStatus(ImMessageStatus.SENT);
        msg.setFoldedCount(1);
        ReflectionTestUtils.setField(msg, "createdAt", LocalDateTime.of(2026, 3, 25, 11, 0, 0));

        doNothing().when(sessionGuardService).ensureMember(77L, admin);
        when(messageRepository.findByIdAndSessionId(889L, 77L)).thenReturn(Optional.of(msg));
        when(messageRepository.save(msg)).thenReturn(msg);

        var event = service.recallMessage(77L, new RecallMessageRequest(889L), admin);
        assertEquals("IM_MESSAGE_RECALLED", event.eventType());
        assertEquals(ImMessageStatus.RECALLED, msg.getStatus());
    }

    @Test
    void establishSessionCountsOnlySentMessagesNotArchived() {
        User actor = user(22L, "reader", false);
        ImSession session = session(900L, actor);

        ImSessionMember member = new ImSessionMember();
        member.setSession(session);
        member.setUser(actor);

        when(sessionMemberRepository.findBySessionIdAndUserId(900L, 22L)).thenReturn(Optional.of(member));
        when(messageRepository.countBySessionIdAndStatus(900L, ImMessageStatus.SENT)).thenReturn(3L);

        var established = service.establishSession(900L, actor);
        assertEquals(3L, established.unreadCount());
        assertTrue(established.unreadCount() >= 0);
    }

    @Test
    void establishSessionAfterLastReadStillCountsOnlySentMessages() {
        User actor = user(23L, "reader2", false);
        ImSession session = session(901L, actor);

        ImMessage lastRead = new ImMessage();
        ReflectionTestUtils.setField(lastRead, "id", 1000L);
        lastRead.setSession(session);
        lastRead.setSender(actor);
        lastRead.setStatus(ImMessageStatus.SENT);

        ImSessionMember member = new ImSessionMember();
        member.setSession(session);
        member.setUser(actor);
        member.setLastReadMessage(lastRead);

        when(sessionMemberRepository.findBySessionIdAndUserId(901L, 23L)).thenReturn(Optional.of(member));
        when(messageRepository.countBySessionIdAndIdGreaterThanAndStatus(901L, 1000L, ImMessageStatus.SENT)).thenReturn(1L);

        var established = service.establishSession(901L, actor);
        assertEquals(1L, established.unreadCount());
    }

    private ImSession session(Long id, User creator) {
        ImSession session = new ImSession();
        ReflectionTestUtils.setField(session, "id", id);
        session.setCreatedBy(creator);
        return session;
    }

    private User user(Long id, String username, boolean admin) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", id);
        user.setUsername(username);
        if (admin) {
            Role role = new Role();
            ReflectionTestUtils.setField(role, "name", "ROLE_PLATFORM_ADMIN");
            user.getRoles().add(role);
        } else {
            user.getRoles().addAll(new HashSet<>());
        }
        return user;
    }
}
