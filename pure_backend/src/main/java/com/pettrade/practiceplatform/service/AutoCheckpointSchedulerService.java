package com.pettrade.practiceplatform.service;

import com.pettrade.practiceplatform.domain.PracticeSession;
import com.pettrade.practiceplatform.domain.enumtype.SessionStatus;
import com.pettrade.practiceplatform.repository.PracticeSessionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AutoCheckpointSchedulerService {

    private final PracticeSessionRepository sessionRepository;
    private final CheckpointService checkpointService;

    public AutoCheckpointSchedulerService(PracticeSessionRepository sessionRepository, CheckpointService checkpointService) {
        this.sessionRepository = sessionRepository;
        this.checkpointService = checkpointService;
    }

    @Transactional
    @Scheduled(fixedDelayString = "${app.scheduler.checkpoint-auto-save-ms:30000}")
    public void autoSaveRunningSessions() {
        List<PracticeSession> sessions = sessionRepository.findByStatus(SessionStatus.IN_PROGRESS);
        for (PracticeSession session : sessions) {
            checkpointService.autoSaveForSession(session);
        }
    }
}
