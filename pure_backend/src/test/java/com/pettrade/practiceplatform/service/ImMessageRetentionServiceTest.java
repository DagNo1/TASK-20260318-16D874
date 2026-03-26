package com.pettrade.practiceplatform.service;

import com.pettrade.practiceplatform.domain.enumtype.ImMessageStatus;
import com.pettrade.practiceplatform.repository.ImMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImMessageRetentionServiceTest {

    @Mock
    private ImMessageRepository imMessageRepository;

    private ImMessageRetentionService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-03-26T03:00:00Z"), ZoneOffset.UTC);
        service = new ImMessageRetentionService(imMessageRepository, clock, 180);
    }

    @Test
    void archivesOnlyMessagesOlderThanConfiguredCutoff() {
        when(imMessageRepository.archiveOldSentMessages(
                eq(ImMessageStatus.SENT),
                eq(ImMessageStatus.ARCHIVED),
                eq(LocalDateTime.of(2026, 3, 26, 3, 0, 0)),
                eq(LocalDateTime.of(2025, 9, 27, 3, 0, 0))
        )).thenReturn(4);

        int archived = service.archiveExpiredMessages();

        assertEquals(4, archived);
        verify(imMessageRepository).archiveOldSentMessages(
                ImMessageStatus.SENT,
                ImMessageStatus.ARCHIVED,
                LocalDateTime.of(2026, 3, 26, 3, 0, 0),
                LocalDateTime.of(2025, 9, 27, 3, 0, 0)
        );
    }
}
