package com.pettrade.practiceplatform.repository;

import com.pettrade.practiceplatform.domain.ReportGeneration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface ReportGenerationRepository extends JpaRepository<ReportGeneration, Long> {
    Optional<ReportGeneration> findByGeneratedForDateAndTimezone(LocalDate generatedForDate, String timezone);
}
