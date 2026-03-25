package com.pettrade.practiceplatform.service;

import com.pettrade.practiceplatform.api.reporting.ReportAggregateView;
import com.pettrade.practiceplatform.api.reporting.ReportExportRequest;
import com.pettrade.practiceplatform.api.reporting.ReportIndicatorView;
import com.pettrade.practiceplatform.api.reporting.ReportQueryRequest;
import com.pettrade.practiceplatform.domain.InventoryAlert;
import com.pettrade.practiceplatform.domain.InventoryLog;
import com.pettrade.practiceplatform.domain.ReportAggregate;
import com.pettrade.practiceplatform.domain.ReportGeneration;
import com.pettrade.practiceplatform.domain.ReportIndicatorDefinition;
import com.pettrade.practiceplatform.repository.InventoryAlertRepository;
import com.pettrade.practiceplatform.repository.InventoryLogRepository;
import com.pettrade.practiceplatform.repository.ReportAggregateRepository;
import com.pettrade.practiceplatform.repository.ReportGenerationRepository;
import com.pettrade.practiceplatform.repository.ReportIndicatorDefinitionRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ReportingService {

    private final ReportIndicatorDefinitionRepository indicatorRepository;
    private final ReportGenerationRepository generationRepository;
    private final ReportAggregateRepository aggregateRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final InventoryAlertRepository inventoryAlertRepository;
    private final Clock clock;
    private final String reportTimezone;

    public ReportingService(
            ReportIndicatorDefinitionRepository indicatorRepository,
            ReportGenerationRepository generationRepository,
            ReportAggregateRepository aggregateRepository,
            InventoryLogRepository inventoryLogRepository,
            InventoryAlertRepository inventoryAlertRepository,
            Clock clock,
            @Value("${app.scheduler.report-timezone:UTC}") String reportTimezone
    ) {
        this.indicatorRepository = indicatorRepository;
        this.generationRepository = generationRepository;
        this.aggregateRepository = aggregateRepository;
        this.inventoryLogRepository = inventoryLogRepository;
        this.inventoryAlertRepository = inventoryAlertRepository;
        this.clock = clock;
        this.reportTimezone = reportTimezone;
    }

    @Transactional(readOnly = true)
    public List<ReportIndicatorView> listIndicators() {
        return indicatorRepository.findByEnabledTrue().stream()
                .map(i -> new ReportIndicatorView(i.getCode(), i.getName(), i.getDescription()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReportAggregateView> queryAggregates(ReportQueryRequest request) {
        if (request.businessDimension() == null || request.businessDimension().isBlank()) {
            return aggregateRepository
                    .findByIndicatorCodeAndOrganizationUserIdAndPeriodStartGreaterThanEqualAndPeriodEndLessThanEqual(
                            request.indicatorCode().trim().toUpperCase(),
                            request.organizationUserId(),
                            request.periodStart(),
                            request.periodEnd()
                    )
                    .stream()
                    .map(this::toView)
                    .toList();
        }
        return aggregateRepository
                .findByIndicatorCodeAndOrganizationUserIdAndBusinessDimensionAndPeriodStartGreaterThanEqualAndPeriodEndLessThanEqual(
                        request.indicatorCode().trim().toUpperCase(),
                        request.organizationUserId(),
                        request.businessDimension().trim().toUpperCase(),
                        request.periodStart(),
                        request.periodEnd()
                )
                .stream()
                .map(this::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> drillDown(ReportQueryRequest request) {
        String indicator = request.indicatorCode().trim().toUpperCase();
        if ("LOW_STOCK_ALERT_EVENTS".equals(indicator)) {
            List<InventoryAlert> alerts = inventoryAlertRepository
                    .findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(request.periodStart(), request.periodEnd());
            return alerts.stream()
                    .filter(a -> a.getItem().getMerchantUser().getId().equals(request.organizationUserId()))
                    .map(a -> Map.<String, Object>of(
                            "alertId", a.getId(),
                            "sku", a.getItem().getSku(),
                            "stock", a.getStockQuantity(),
                            "threshold", a.getThresholdValue(),
                            "createdAt", a.getCreatedAt().toString()
                    ))
                    .toList();
        }
        List<InventoryLog> logs = inventoryLogRepository
                .findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(request.periodStart(), request.periodEnd());
        return logs.stream()
                .filter(l -> l.getItem().getMerchantUser().getId().equals(request.organizationUserId()))
                .map(l -> Map.<String, Object>of(
                        "logId", l.getId(),
                        "sku", l.getItem().getSku(),
                        "prev", l.getPreviousQuantity(),
                        "next", l.getNewQuantity(),
                        "reason", l.getChangeReason() == null ? "" : l.getChangeReason(),
                        "createdAt", l.getCreatedAt().toString()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public byte[] exportXlsx(ReportExportRequest request) {
        ReportQueryRequest query = new ReportQueryRequest(
                request.indicatorCode(),
                request.organizationUserId(),
                request.businessDimension(),
                request.periodStart(),
                request.periodEnd()
        );
        List<ReportAggregateView> rows = queryAggregates(query);

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("report");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Indicator");
            header.createCell(1).setCellValue("OrgUserId");
            header.createCell(2).setCellValue("Dimension");
            header.createCell(3).setCellValue("DimensionValue");
            header.createCell(4).setCellValue("PeriodStart");
            header.createCell(5).setCellValue("PeriodEnd");
            header.createCell(6).setCellValue("Value");

            for (int i = 0; i < rows.size(); i++) {
                ReportAggregateView v = rows.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(v.indicatorCode());
                row.createCell(1).setCellValue(v.organizationUserId());
                row.createCell(2).setCellValue(v.businessDimension());
                row.createCell(3).setCellValue(v.dimensionValue());
                row.createCell(4).setCellValue(v.periodStart().toString());
                row.createCell(5).setCellValue(v.periodEnd().toString());
                row.createCell(6).setCellValue(v.aggregatedValue().doubleValue());
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to export report xlsx", e);
        }
    }

    @Transactional
    @Scheduled(cron = "0 0 2 * * *", zone = "${app.scheduler.report-timezone:UTC}")
    public void generateDailyReportAt2am() {
        ZoneId zone = ZoneId.of(reportTimezone);
        LocalDate reportDate = LocalDate.now(clock.withZone(zone)).minusDays(1);
        Optional<ReportGeneration> existing = generationRepository.findByGeneratedForDateAndTimezone(reportDate, reportTimezone);
        if (existing.isPresent()) {
            return;
        }

        ReportGeneration generation = new ReportGeneration();
        generation.setGeneratedForDate(reportDate);
        generation.setTimezone(reportTimezone);
        generation.setStatus("COMPLETED");
        generation = generationRepository.save(generation);

        LocalDateTime periodStart = reportDate.atStartOfDay();
        LocalDateTime periodEnd = reportDate.plusDays(1).atStartOfDay();

        List<InventoryLog> logs = inventoryLogRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(periodStart, periodEnd);
        List<InventoryAlert> alerts = inventoryAlertRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(periodStart, periodEnd);

        List<ReportAggregate> aggregates = new ArrayList<>();
        aggregates.addAll(buildLogBasedAggregates(generation, logs, periodStart, periodEnd));
        aggregates.addAll(buildAlertBasedAggregates(generation, alerts, periodStart, periodEnd));
        aggregateRepository.saveAll(aggregates);
    }

    private List<ReportAggregate> buildLogBasedAggregates(
            ReportGeneration generation,
            List<InventoryLog> logs,
            LocalDateTime periodStart,
            LocalDateTime periodEnd
    ) {
        java.util.Map<Long, java.util.Map<String, BigDecimal>> byOrg = new java.util.HashMap<>();
        for (InventoryLog log : logs) {
            Long orgId = log.getItem().getMerchantUser().getId();
            byOrg.putIfAbsent(orgId, new java.util.HashMap<>());
            java.util.Map<String, BigDecimal> values = byOrg.get(orgId);
            values.merge("INVENTORY_CHANGE_EVENTS", BigDecimal.ONE, BigDecimal::add);
            BigDecimal delta = BigDecimal.valueOf(log.getNewQuantity() - log.getPreviousQuantity());
            values.merge("INVENTORY_NET_STOCK_DELTA", delta, BigDecimal::add);
        }

        List<ReportAggregate> out = new ArrayList<>();
        byOrg.forEach((orgId, map) -> map.forEach((indicator, value) -> {
            ReportAggregate a = new ReportAggregate();
            a.setGeneration(generation);
            a.setIndicatorCode(indicator);
            a.setOrganizationUserId(orgId);
            a.setBusinessDimension("INVENTORY");
            a.setDimensionValue("ALL_SKU");
            a.setPeriodStart(periodStart);
            a.setPeriodEnd(periodEnd);
            a.setAggregatedValue(value);
            out.add(a);
        }));
        return out;
    }

    private List<ReportAggregate> buildAlertBasedAggregates(
            ReportGeneration generation,
            List<InventoryAlert> alerts,
            LocalDateTime periodStart,
            LocalDateTime periodEnd
    ) {
        java.util.Map<Long, BigDecimal> counts = new java.util.HashMap<>();
        for (InventoryAlert alert : alerts) {
            Long orgId = alert.getItem().getMerchantUser().getId();
            counts.merge(orgId, BigDecimal.ONE, BigDecimal::add);
        }
        List<ReportAggregate> out = new ArrayList<>();
        counts.forEach((orgId, count) -> {
            ReportAggregate a = new ReportAggregate();
            a.setGeneration(generation);
            a.setIndicatorCode("LOW_STOCK_ALERT_EVENTS");
            a.setOrganizationUserId(orgId);
            a.setBusinessDimension("INVENTORY_ALERT");
            a.setDimensionValue("ALL");
            a.setPeriodStart(periodStart);
            a.setPeriodEnd(periodEnd);
            a.setAggregatedValue(count);
            out.add(a);
        });
        return out;
    }

    private ReportAggregateView toView(ReportAggregate a) {
        return new ReportAggregateView(
                a.getId(),
                a.getIndicatorCode(),
                a.getOrganizationUserId(),
                a.getBusinessDimension(),
                a.getDimensionValue(),
                a.getPeriodStart(),
                a.getPeriodEnd(),
                a.getAggregatedValue()
        );
    }
}
