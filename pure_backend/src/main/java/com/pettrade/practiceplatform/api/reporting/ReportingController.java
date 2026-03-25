package com.pettrade.practiceplatform.api.reporting;

import com.pettrade.practiceplatform.service.ReportingService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reporting")
public class ReportingController {

    private final ReportingService reportingService;

    public ReportingController(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping("/indicators")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','MERCHANT_OPERATOR','REVIEWER')")
    public ResponseEntity<List<ReportIndicatorView>> listIndicators() {
        return ResponseEntity.ok(reportingService.listIndicators());
    }

    @PostMapping("/query")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','MERCHANT_OPERATOR','REVIEWER')")
    public ResponseEntity<List<ReportAggregateView>> query(@Valid @RequestBody ReportQueryRequest request) {
        return ResponseEntity.ok(reportingService.queryAggregates(request));
    }

    @PostMapping("/drill-down")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','MERCHANT_OPERATOR','REVIEWER')")
    public ResponseEntity<List<Map<String, Object>>> drillDown(@Valid @RequestBody ReportQueryRequest request) {
        return ResponseEntity.ok(reportingService.drillDown(request));
    }

    @PostMapping("/export")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','MERCHANT_OPERATOR','REVIEWER')")
    public ResponseEntity<byte[]> export(@Valid @RequestBody ReportExportRequest request) {
        byte[] data = reportingService.exportXlsx(request);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment().filename("report.xlsx").build());
        return ResponseEntity.ok().headers(headers).body(data);
    }

    @PostMapping("/generate-daily")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN')")
    public ResponseEntity<Void> generateDaily() {
        reportingService.generateDailyReportAt2am();
        return ResponseEntity.accepted().build();
    }
}
