package com.pettrade.practiceplatform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pettrade.practiceplatform.api.practice.CheckpointResponse;
import com.pettrade.practiceplatform.domain.PracticeSession;
import com.pettrade.practiceplatform.domain.PracticeStep;
import com.pettrade.practiceplatform.domain.SessionCheckpoint;
import com.pettrade.practiceplatform.domain.StepTimer;
import com.pettrade.practiceplatform.domain.User;
import com.pettrade.practiceplatform.domain.enumtype.CheckpointType;
import com.pettrade.practiceplatform.domain.enumtype.SessionStatus;
import com.pettrade.practiceplatform.domain.enumtype.StepStatus;
import com.pettrade.practiceplatform.domain.enumtype.TimerState;
import com.pettrade.practiceplatform.repository.PracticeSessionRepository;
import com.pettrade.practiceplatform.repository.PracticeStepRepository;
import com.pettrade.practiceplatform.repository.SessionCheckpointRepository;
import com.pettrade.practiceplatform.repository.StepTimerRepository;
import com.pettrade.practiceplatform.service.timer.TimerStateMachine;
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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckpointServiceTest {

    @Mock
    private PracticeSessionRepository sessionRepository;
    @Mock
    private PracticeStepRepository stepRepository;
    @Mock
    private StepTimerRepository timerRepository;
    @Mock
    private SessionCheckpointRepository checkpointRepository;

    private CheckpointService checkpointService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-03-25T12:00:00Z"), ZoneOffset.UTC);
        checkpointService = new CheckpointService(
                sessionRepository,
                stepRepository,
                timerRepository,
                checkpointRepository,
                new ObjectMapper(),
                new TimerStateMachine(),
                clock
        );
    }

    @Test
    void loadLatestCheckpointReconcilesRunningTimerAndCreatesFreshSnapshot() {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", 11L);

        PracticeSession session = new PracticeSession();
        session.setUser(user);
        session.setTitle("Session A");
        session.setStatus(SessionStatus.IN_PROGRESS);
        ReflectionTestUtils.setField(session, "id", 100L);

        PracticeStep step = new PracticeStep();
        ReflectionTestUtils.setField(step, "id", 200L);
        step.setStatus(StepStatus.IN_PROGRESS);
        step.setSession(session);

        StepTimer timer = new StepTimer();
        ReflectionTestUtils.setField(timer, "id", 300L);
        timer.setStep(step);
        timer.setTimerKey("boil");
        timer.setState(TimerState.RUNNING);
        timer.setRemainingSeconds(10L);
        timer.setDueAt(LocalDateTime.of(2026, 3, 25, 11, 59, 55));

        when(sessionRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(session));
        when(timerRepository.findByStepSessionId(100L)).thenReturn(List.of(timer));
        when(stepRepository.findBySessionIdOrderByStepOrderAsc(100L)).thenReturn(List.of(step));
        when(timerRepository.findByStepId(200L)).thenReturn(List.of(timer));

        when(checkpointRepository.save(any(SessionCheckpoint.class))).thenAnswer(invocation -> {
            SessionCheckpoint cp = invocation.getArgument(0);
            ReflectionTestUtils.setField(cp, "id", 999L);
            ReflectionTestUtils.setField(cp, "createdAt", LocalDateTime.of(2026, 3, 25, 12, 0, 0));
            return cp;
        });

        CheckpointResponse response = checkpointService.loadLatestCheckpoint(100L, user);

        ArgumentCaptor<SessionCheckpoint> captor = ArgumentCaptor.forClass(SessionCheckpoint.class);
        org.mockito.Mockito.verify(checkpointRepository).save(captor.capture());
        String snapshot = captor.getValue().getSnapshotJson();

        assertTrue(snapshot.contains("\"timerId\":300"));
        assertTrue(snapshot.contains("\"state\":\"COMPLETED\""));
        assertTrue(snapshot.contains("\"remainingSeconds\":0"));
        assertTrue("AUTO".equals(response.type()));
    }
}
