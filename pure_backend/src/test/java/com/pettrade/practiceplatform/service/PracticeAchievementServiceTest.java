package com.pettrade.practiceplatform.service;

import com.pettrade.practiceplatform.api.achievement.AchievementUpsertRequest;
import com.pettrade.practiceplatform.domain.PracticeAchievement;
import com.pettrade.practiceplatform.domain.User;
import com.pettrade.practiceplatform.exception.BusinessRuleException;
import com.pettrade.practiceplatform.repository.PracticeAchievementAttachmentRepository;
import com.pettrade.practiceplatform.repository.PracticeAchievementRepository;
import com.pettrade.practiceplatform.repository.PracticeAssessmentFormRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PracticeAchievementServiceTest {

    @Mock
    private PracticeAchievementRepository achievementRepository;
    @Mock
    private PracticeAchievementAttachmentRepository attachmentRepository;
    @Mock
    private PracticeAssessmentFormRepository formRepository;

    private PracticeAchievementService service;

    @BeforeEach
    void setUp() {
        service = new PracticeAchievementService(achievementRepository, attachmentRepository, formRepository);
    }

    @Test
    void updateRejectsRollbackOrSameVersion() {
        User owner = user(55L);
        PracticeAchievement existing = achievement(100L, owner, 3L);

        when(achievementRepository.findByIdAndOwnerUserId(100L, 55L)).thenReturn(Optional.of(existing));

        AchievementUpsertRequest sameVersion = new AchievementUpsertRequest(
                3L,
                "Title",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31),
                "Person",
                "Conclusion"
        );
        assertThrows(BusinessRuleException.class, () -> service.updateAchievement(100L, sameVersion, owner));

        AchievementUpsertRequest lowerVersion = new AchievementUpsertRequest(
                2L,
                "Title",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31),
                "Person",
                "Conclusion"
        );
        assertThrows(BusinessRuleException.class, () -> service.updateAchievement(100L, lowerVersion, owner));
    }

    @Test
    void updateAcceptsStrictIncrement() {
        User owner = user(56L);
        PracticeAchievement existing = achievement(101L, owner, 4L);

        when(achievementRepository.findByIdAndOwnerUserId(101L, 56L)).thenReturn(Optional.of(existing));
        when(achievementRepository.save(any(PracticeAchievement.class))).thenAnswer(inv -> inv.getArgument(0));

        AchievementUpsertRequest next = new AchievementUpsertRequest(
                5L,
                "New Title",
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 28),
                "Lead Mentor",
                "Great completion"
        );

        var result = service.updateAchievement(101L, next, owner);
        assertEquals(5L, result.versionNo());
        assertEquals("New Title", result.title());
    }

    private User user(Long id) {
        User u = new User();
        ReflectionTestUtils.setField(u, "id", id);
        u.setUsername("u" + id);
        return u;
    }

    private PracticeAchievement achievement(Long id, User owner, Long version) {
        PracticeAchievement a = new PracticeAchievement();
        ReflectionTestUtils.setField(a, "id", id);
        a.setOwnerUser(owner);
        a.setVersionNo(version);
        a.setTitle("old");
        a.setPeriodStart(LocalDate.of(2026, 1, 1));
        a.setPeriodEnd(LocalDate.of(2026, 1, 2));
        a.setResponsiblePerson("r");
        a.setConclusion("c");
        return a;
    }
}
