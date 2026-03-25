package com.pettrade.practiceplatform.repository;

import com.pettrade.practiceplatform.domain.ReportIndicatorDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportIndicatorDefinitionRepository extends JpaRepository<ReportIndicatorDefinition, Long> {
    List<ReportIndicatorDefinition> findByEnabledTrue();
}
