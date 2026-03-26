package com.pettrade.practiceplatform.service;

import com.pettrade.practiceplatform.domain.enumtype.ImMessageStatus;
import com.pettrade.practiceplatform.repository.ImMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class ImMessageRetentionService {

    private static final Logger log = LoggerFactory.getLogger(ImMessageRetentionService.class);

    private final ImMessageRepository imMessageRepository;
    private final Clock clock;
    private final int retentionDays;

    public ImMessageRetentionService(
            ImMessageRepository imMessageRepository,
            Clock clock,
            @Value("${app.im.retention-days:180}") int retentionDays
    ) {
        this.imMessageRepository = imMessageRepository;
        this.clock = clock;
        this.retentionDays = retentionDays;
    }

    @Transactional
    @Scheduled(cron = "${app.im.retention-cron:0 30 2 * * *}")
    public int archiveExpiredMessages() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime cutoff = now.minusDays(retentionDays);
        int archived = imMessageRepository.archiveOldSentMessages(
                ImMessageStatus.SENT,
                ImMessageStatus.ARCHIVED,
                now,
                cutoff
        );
        log.info("IM retention archived {} messages older than {} days", archived, retentionDays);
        return archived;
    }
}
