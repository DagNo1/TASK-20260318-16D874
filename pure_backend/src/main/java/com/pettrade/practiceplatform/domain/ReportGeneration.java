package com.pettrade.practiceplatform.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "report_generations")
public class ReportGeneration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "generated_for_date", nullable = false)
    private LocalDate generatedForDate;

    @Column(nullable = false, length = 80)
    private String timezone;

    @Column(nullable = false, length = 30)
    private String status;

    @CreationTimestamp
    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    public Long getId() {
        return id;
    }

    public LocalDate getGeneratedForDate() {
        return generatedForDate;
    }

    public void setGeneratedForDate(LocalDate generatedForDate) {
        this.generatedForDate = generatedForDate;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
