package com.pettrade.practiceplatform.service;

import com.pettrade.practiceplatform.api.inventory.InventoryAlertView;
import com.pettrade.practiceplatform.api.inventory.InventoryItemUpsertRequest;
import com.pettrade.practiceplatform.api.inventory.InventoryItemView;
import com.pettrade.practiceplatform.api.inventory.InventoryLogView;
import com.pettrade.practiceplatform.api.inventory.InventoryStockAdjustRequest;
import com.pettrade.practiceplatform.domain.InventoryAlert;
import com.pettrade.practiceplatform.domain.InventoryItem;
import com.pettrade.practiceplatform.domain.InventoryLog;
import com.pettrade.practiceplatform.domain.User;
import com.pettrade.practiceplatform.domain.enumtype.InventoryAlertStatus;
import com.pettrade.practiceplatform.exception.NotFoundException;
import com.pettrade.practiceplatform.repository.InventoryAlertRepository;
import com.pettrade.practiceplatform.repository.InventoryItemRepository;
import com.pettrade.practiceplatform.repository.InventoryLogRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class InventoryService {

    private final InventoryItemRepository itemRepository;
    private final InventoryLogRepository logRepository;
    private final InventoryAlertRepository alertRepository;
    private final Clock clock;
    private final long defaultThreshold;

    public InventoryService(
            InventoryItemRepository itemRepository,
            InventoryLogRepository logRepository,
            InventoryAlertRepository alertRepository,
            Clock clock,
            @Value("${app.inventory.default-threshold:10}") long defaultThreshold
    ) {
        this.itemRepository = itemRepository;
        this.logRepository = logRepository;
        this.alertRepository = alertRepository;
        this.clock = clock;
        this.defaultThreshold = defaultThreshold;
    }

    @Transactional
    public InventoryItemView upsertItem(InventoryItemUpsertRequest request, User merchant) {
        InventoryItem item = itemRepository.findByMerchantUserIdAndSku(merchant.getId(), request.sku())
                .orElseGet(() -> {
                    InventoryItem i = new InventoryItem();
                    i.setMerchantUser(merchant);
                    i.setSku(request.sku());
                    return i;
                });

        Long previous = item.getStockQuantity() == null ? 0L : item.getStockQuantity();
        item.setName(request.name());
        item.setStockQuantity(request.stockQuantity());
        item.setAlertThreshold(request.alertThreshold());
        InventoryItem saved = itemRepository.save(item);

        writeLog(saved, previous, request.stockQuantity(), "UPSERT", merchant);
        evaluateAlert(saved, merchant);
        return toItemView(saved);
    }

    @Transactional
    public InventoryItemView adjustStock(Long itemId, InventoryStockAdjustRequest request, User merchant) {
        InventoryItem item = itemRepository.findByIdAndMerchantUserId(itemId, merchant.getId())
                .orElseThrow(() -> new NotFoundException("Inventory item not found"));

        Long previous = item.getStockQuantity();
        item.setStockQuantity(request.newQuantity());
        InventoryItem saved = itemRepository.save(item);

        writeLog(saved, previous, request.newQuantity(), request.reason(), merchant);
        evaluateAlert(saved, merchant);
        return toItemView(saved);
    }

    @Transactional(readOnly = true)
    public List<InventoryAlertView> listAlerts(User merchant) {
        return alertRepository.findByItemMerchantUserIdOrderByCreatedAtDesc(merchant.getId())
                .stream()
                .map(this::toAlertView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InventoryLogView> listLogs(User merchant) {
        return logRepository.findByItemMerchantUserIdOrderByIdDesc(merchant.getId())
                .stream()
                .map(this::toLogView)
                .toList();
    }

    private void evaluateAlert(InventoryItem item, User merchant) {
        long threshold = item.getAlertThreshold() == null ? defaultThreshold : item.getAlertThreshold();
        long stock = item.getStockQuantity();
        LocalDateTime now = LocalDateTime.now(clock);

        if (stock <= threshold) {
            InventoryAlert open = alertRepository.findTopByItemIdAndStatusOrderByCreatedAtDesc(item.getId(), InventoryAlertStatus.OPEN)
                    .orElse(null);
            if (open == null) {
                InventoryAlert alert = new InventoryAlert();
                alert.setItem(item);
                alert.setThresholdValue(threshold);
                alert.setStockQuantity(stock);
                alert.setStatus(InventoryAlertStatus.OPEN);
                alert.setMessage("Stock low for SKU " + item.getSku() + ": quantity=" + stock + ", threshold=" + threshold);
                alertRepository.save(alert);
            } else {
                open.setStockQuantity(stock);
                open.setThresholdValue(threshold);
                open.setMessage("Stock low for SKU " + item.getSku() + ": quantity=" + stock + ", threshold=" + threshold);
                alertRepository.save(open);
            }
        } else {
            alertRepository.findTopByItemIdAndStatusOrderByCreatedAtDesc(item.getId(), InventoryAlertStatus.OPEN)
                    .ifPresent(open -> {
                        open.setStatus(InventoryAlertStatus.RESOLVED);
                        open.setResolvedAt(now);
                        alertRepository.save(open);
                    });
        }
    }

    private void writeLog(InventoryItem item, Long previous, Long next, String reason, User actor) {
        InventoryLog log = new InventoryLog();
        log.setItem(item);
        log.setPreviousQuantity(previous);
        log.setNewQuantity(next);
        log.setChangeReason(reason);
        log.setChangedByUser(actor);
        logRepository.save(log);
    }

    private InventoryItemView toItemView(InventoryItem item) {
        return new InventoryItemView(item.getId(), item.getSku(), item.getName(), item.getStockQuantity(), item.getAlertThreshold());
    }

    private InventoryAlertView toAlertView(InventoryAlert alert) {
        return new InventoryAlertView(
                alert.getId(),
                alert.getItem().getId(),
                alert.getItem().getSku(),
                alert.getThresholdValue(),
                alert.getStockQuantity(),
                alert.getStatus().name(),
                alert.getMessage(),
                alert.getCreatedAt(),
                alert.getResolvedAt()
        );
    }

    private InventoryLogView toLogView(InventoryLog log) {
        return new InventoryLogView(
                log.getId(),
                log.getItem().getId(),
                log.getItem().getSku(),
                log.getPreviousQuantity(),
                log.getNewQuantity(),
                log.getChangeReason(),
                log.getChangedByUser().getId(),
                log.getCreatedAt()
        );
    }
}
