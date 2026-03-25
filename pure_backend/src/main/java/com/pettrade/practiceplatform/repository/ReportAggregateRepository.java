package com.pettrade.practiceplatform.repository;

import com.pettrade.practiceplatform.domain.ReportAggregate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ReportAggregateRepository extends JpaRepository<ReportAggregate, Long> {
    List<ReportAggregate> findByIndicatorCodeAndOrganizationUserIdAndBusinessDimensionAndPeriodStartGreaterThanEqualAndPeriodEndLessThanEqual(
            String indicatorCode,
            Long organizationUserId,
            String businessDimension,
            LocalDateTime periodStart,
            LocalDateTime periodEnd
    );

    List<ReportAggregate> findByIndicatorCodeAndOrganizationUserIdAndPeriodStartGreaterThanEqualAndPeriodEndLessThanEqual(
            String indicatorCode,
            Long organizationUserId,
            LocalDateTime periodStart,
            LocalDateTime periodEnd
    );

    List<ReportAggregate> findByGenerationId(Long generationId);
}
