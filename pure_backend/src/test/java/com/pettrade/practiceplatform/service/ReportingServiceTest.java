package com.pettrade.practiceplatform.service;

import com.pettrade.practiceplatform.domain.InventoryAlert;
import com.pettrade.practiceplatform.domain.InventoryItem;
import com.pettrade.practiceplatform.domain.InventoryLog;
import com.pettrade.practiceplatform.domain.Role;
import com.pettrade.practiceplatform.domain.ReportAggregate;
import com.pettrade.practiceplatform.domain.ReportGeneration;
import com.pettrade.practiceplatform.domain.User;
import com.pettrade.practiceplatform.repository.InventoryAlertRepository;
import com.pettrade.practiceplatform.repository.InventoryLogRepository;
import com.pettrade.practiceplatform.repository.ReportAggregateRepository;
import com.pettrade.practiceplatform.repository.ReportGenerationRepository;
import com.pettrade.practiceplatform.repository.ReportIndicatorDefinitionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportingServiceTest {

    @Mock
    private ReportIndicatorDefinitionRepository indicatorRepository;
    @Mock
    private ReportGenerationRepository generationRepository;
    @Mock
    private ReportAggregateRepository aggregateRepository;
    @Mock
    private InventoryLogRepository inventoryLogRepository;
    @Mock
    private InventoryAlertRepository inventoryAlertRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private CurrentUserService currentUserService;

    private ReportingService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-03-26T03:00:00Z"), ZoneOffset.UTC);
        service = new ReportingService(
                indicatorRepository,
                generationRepository,
                aggregateRepository,
                inventoryLogRepository,
                inventoryAlertRepository,
                notificationService,
                currentUserService,
                clock,
                "UTC"
        );
    }

    @Test
    void dailyAggregationBuildsExpectedIndicatorValues() {
        LocalDate reportDate = LocalDate.of(2026, 3, 25);
        LocalDateTime start = reportDate.atStartOfDay();
        LocalDateTime end = reportDate.plusDays(1).atStartOfDay();

        when(generationRepository.findByGeneratedForDateAndTimezone(reportDate, "UTC")).thenReturn(Optional.empty());
        when(generationRepository.save(any(ReportGeneration.class))).thenAnswer(inv -> {
            ReportGeneration g = inv.getArgument(0);
            ReflectionTestUtils.setField(g, "id", 100L);
            return g;
        });

        InventoryLog l1 = log(1L, "SKU-A", 10L, 7L); // delta -3
        InventoryLog l2 = log(1L, "SKU-B", 5L, 8L);  // delta +3 => net 0 org1, count2
        InventoryLog l3 = log(2L, "SKU-C", 9L, 6L);  // delta -3 org2, count1

        InventoryAlert a1 = alert(1L, "SKU-A");
        InventoryAlert a2 = alert(2L, "SKU-C");
        InventoryAlert a3 = alert(2L, "SKU-D");

        when(inventoryLogRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(start, end)).thenReturn(List.of(l1, l2, l3));
        when(inventoryAlertRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(start, end)).thenReturn(List.of(a1, a2, a3));

        service.generateDailyReportAt2am();

        ArgumentCaptor<List<ReportAggregate>> captor = ArgumentCaptor.forClass(List.class);
        verify(aggregateRepository).saveAll(captor.capture());
        List<ReportAggregate> saved = captor.getValue();

        BigDecimal org1Count = value(saved, "INVENTORY_CHANGE_EVENTS", 1L);
        BigDecimal org1Delta = value(saved, "INVENTORY_NET_STOCK_DELTA", 1L);
        BigDecimal org2Count = value(saved, "INVENTORY_CHANGE_EVENTS", 2L);
        BigDecimal org2Delta = value(saved, "INVENTORY_NET_STOCK_DELTA", 2L);
        BigDecimal org1Alerts = value(saved, "LOW_STOCK_ALERT_EVENTS", 1L);
        BigDecimal org2Alerts = value(saved, "LOW_STOCK_ALERT_EVENTS", 2L);

        assertEquals(new BigDecimal("2"), org1Count.stripTrailingZeros());
        assertEquals(new BigDecimal("0"), org1Delta.stripTrailingZeros());
        assertEquals(new BigDecimal("1"), org2Count.stripTrailingZeros());
        assertEquals(new BigDecimal("-3"), org2Delta.stripTrailingZeros());
        assertEquals(new BigDecimal("1"), org1Alerts.stripTrailingZeros());
        assertEquals(new BigDecimal("2"), org2Alerts.stripTrailingZeros());
    }

    @Test
    void nonAdminCannotQueryAnotherOrganizationScope() {
        when(currentUserService.currentUser()).thenReturn(user(7L, "ROLE_MERCHANT_OPERATOR"));

        assertThrows(AccessDeniedException.class, () -> service.queryAggregates(
                new com.pettrade.practiceplatform.api.reporting.ReportQueryRequest(
                        "INVENTORY_CHANGE_EVENTS",
                        9L,
                        null,
                        LocalDateTime.of(2026, 3, 25, 0, 0),
                        LocalDateTime.of(2026, 3, 26, 0, 0)
                )
        ));
    }

    @Test
    void nonAdminCannotExportAnotherOrganizationScope() {
        when(currentUserService.currentUser()).thenReturn(user(7L, "ROLE_MERCHANT_OPERATOR"));

        assertThrows(AccessDeniedException.class, () -> service.exportXlsx(
                new com.pettrade.practiceplatform.api.reporting.ReportExportRequest(
                        "INVENTORY_CHANGE_EVENTS",
                        9L,
                        null,
                        LocalDateTime.of(2026, 3, 25, 0, 0),
                        LocalDateTime.of(2026, 3, 26, 0, 0)
                )
        ));
    }

    private BigDecimal value(List<ReportAggregate> list, String indicator, Long orgId) {
        return list.stream()
                .filter(a -> indicator.equals(a.getIndicatorCode()) && orgId.equals(a.getOrganizationUserId()))
                .findFirst()
                .orElseThrow()
                .getAggregatedValue();
    }

    private InventoryLog log(Long orgId, String sku, Long prev, Long next) {
        InventoryItem item = new InventoryItem();
        item.setMerchantUser(user(orgId));
        item.setSku(sku);

        InventoryLog log = new InventoryLog();
        log.setItem(item);
        log.setPreviousQuantity(prev);
        log.setNewQuantity(next);
        return log;
    }

    private InventoryAlert alert(Long orgId, String sku) {
        InventoryItem item = new InventoryItem();
        item.setMerchantUser(user(orgId));
        item.setSku(sku);

        InventoryAlert alert = new InventoryAlert();
        alert.setItem(item);
        return alert;
    }

    private User user(Long id) {
        return user(id, "ROLE_MERCHANT_OPERATOR");
    }

    private User user(Long id, String roleName) {
        User u = new User();
        ReflectionTestUtils.setField(u, "id", id);
        u.setUsername("u" + id);
        Role role = new Role();
        ReflectionTestUtils.setField(role, "id", id + 1000);
        ReflectionTestUtils.setField(role, "name", roleName);
        u.getRoles().addAll(Set.of(role));
        return u;
    }
}
