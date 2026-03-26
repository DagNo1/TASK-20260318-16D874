package com.pettrade.practiceplatform.service;

import com.pettrade.practiceplatform.domain.ImMessage;
import com.pettrade.practiceplatform.domain.ImSession;
import com.pettrade.practiceplatform.domain.User;
import com.pettrade.practiceplatform.domain.enumtype.ImMessageStatus;
import com.pettrade.practiceplatform.domain.enumtype.ImMessageType;
import com.pettrade.practiceplatform.repository.ImMessageRepository;
import com.pettrade.practiceplatform.repository.ImSessionRepository;
import com.pettrade.practiceplatform.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
@ActiveProfiles("test")
class ImMessageRepositoryRetentionTest {

    @Autowired
    private ImMessageRepository imMessageRepository;
    @Autowired
    private ImSessionRepository imSessionRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EntityManager entityManager;

    @Test
    void archiveOldSentMessagesUsesStrictCutoffAndSkipsRecentOrNonSent() {
        LocalDateTime now = LocalDateTime.of(2026, 3, 26, 3, 0, 0);
        LocalDateTime cutoff = now.minusDays(180);

        User user = createUser("im-retention-user");
        ImSession session = createSession(user);

        ImMessage oldSent = createMessage(session, user, ImMessageStatus.SENT);
        ImMessage boundarySent = createMessage(session, user, ImMessageStatus.SENT);
        ImMessage recentSent = createMessage(session, user, ImMessageStatus.SENT);
        ImMessage oldRecalled = createMessage(session, user, ImMessageStatus.RECALLED);

        setCreatedAt(oldSent.getId(), cutoff.minusSeconds(1));
        setCreatedAt(boundarySent.getId(), cutoff);
        setCreatedAt(recentSent.getId(), cutoff.plusSeconds(1));
        setCreatedAt(oldRecalled.getId(), cutoff.minusSeconds(10));

        int archived = imMessageRepository.archiveOldSentMessages(
                ImMessageStatus.SENT,
                ImMessageStatus.ARCHIVED,
                now,
                cutoff
        );

        assertEquals(1, archived);

        ImMessage updatedOldSent = imMessageRepository.findById(oldSent.getId()).orElseThrow();
        ImMessage updatedBoundarySent = imMessageRepository.findById(boundarySent.getId()).orElseThrow();
        ImMessage updatedRecentSent = imMessageRepository.findById(recentSent.getId()).orElseThrow();
        ImMessage updatedOldRecalled = imMessageRepository.findById(oldRecalled.getId()).orElseThrow();

        assertEquals(ImMessageStatus.ARCHIVED, updatedOldSent.getStatus());
        assertNotNull(updatedOldSent.getArchivedAt());

        assertEquals(ImMessageStatus.SENT, updatedBoundarySent.getStatus());
        assertEquals(ImMessageStatus.SENT, updatedRecentSent.getStatus());
        assertEquals(ImMessageStatus.RECALLED, updatedOldRecalled.getStatus());
    }

    private User createUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash("$2a$10$7EqJtq98hPqEX7fNZaFWoOHiR0Jw4u7v6/9erjRzCQXDpUe1koX6.");
        user.setEnabled(true);
        return userRepository.save(user);
    }

    private ImSession createSession(User user) {
        ImSession session = new ImSession();
        session.setCreatedBy(user);
        session.setTitle("retention-session");
        return imSessionRepository.save(session);
    }

    private ImMessage createMessage(ImSession session, User sender, ImMessageStatus status) {
        ImMessage message = new ImMessage();
        message.setSession(session);
        message.setSender(sender);
        message.setMessageType(ImMessageType.TEXT);
        message.setStatus(status);
        message.setContent("hello");
        message.setDedupKey("k-" + System.nanoTime());
        message.setFoldedCount(1);
        message.setLastFoldedAt(LocalDateTime.of(2026, 3, 25, 0, 0, 0));
        return imMessageRepository.saveAndFlush(message);
    }

    private void setCreatedAt(Long messageId, LocalDateTime createdAt) {
        entityManager.createNativeQuery("update im_messages set created_at = ? where id = ?")
                .setParameter(1, createdAt)
                .setParameter(2, messageId)
                .executeUpdate();
        entityManager.clear();
    }
}
