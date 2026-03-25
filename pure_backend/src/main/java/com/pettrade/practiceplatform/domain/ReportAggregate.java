package com.pettrade.practiceplatform.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "report_aggregates")
public class ReportAggregate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generation_id", nullable = false)
    private ReportGeneration generation;

    @Column(name = "indicator_code", nullable = false, length = 80)
    private String indicatorCode;

    @Column(name = "organization_user_id", nullable = false)
    private Long organizationUserId;

    @Column(name = "business_dimension", nullable = false, length = 60)
    private String businessDimension;

    @Column(name = "dimension_value", nullable = false, length = 255)
    private String dimensionValue;

    @Column(name = "period_start", nullable = false)
    private LocalDateTime periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDateTime periodEnd;

    @Column(name = "aggregated_value", nullable = false, precision = 20, scale = 4)
    private BigDecimal aggregatedValue;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public ReportGeneration getGeneration() {
        return generation;
    }

    public void setGeneration(ReportGeneration generation) {
        this.generation = generation;
    }

    public String getIndicatorCode() {
        return indicatorCode;
    }

    public void setIndicatorCode(String indicatorCode) {
        this.indicatorCode = indicatorCode;
    }

    public Long getOrganizationUserId() {
        return organizationUserId;
    }

    public void setOrganizationUserId(Long organizationUserId) {
        this.organizationUserId = organizationUserId;
    }

    public String getBusinessDimension() {
        return businessDimension;
    }

    public void setBusinessDimension(String businessDimension) {
        this.businessDimension = businessDimension;
    }

    public String getDimensionValue() {
        return dimensionValue;
    }

    public void setDimensionValue(String dimensionValue) {
        this.dimensionValue = dimensionValue;
    }

    public LocalDateTime getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(LocalDateTime periodStart) {
        this.periodStart = periodStart;
    }

    public LocalDateTime getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(LocalDateTime periodEnd) {
        this.periodEnd = periodEnd;
    }

    public BigDecimal getAggregatedValue() {
        return aggregatedValue;
    }

    public void setAggregatedValue(BigDecimal aggregatedValue) {
        this.aggregatedValue = aggregatedValue;
    }
}
