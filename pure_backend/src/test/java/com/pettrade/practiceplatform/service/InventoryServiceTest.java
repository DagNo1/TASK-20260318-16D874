package com.pettrade.practiceplatform.service;

import com.pettrade.practiceplatform.api.inventory.InventoryItemUpsertRequest;
import com.pettrade.practiceplatform.api.inventory.InventoryStockAdjustRequest;
import com.pettrade.practiceplatform.domain.InventoryAlert;
import com.pettrade.practiceplatform.domain.InventoryItem;
import com.pettrade.practiceplatform.domain.User;
import com.pettrade.practiceplatform.domain.enumtype.InventoryAlertStatus;
import com.pettrade.practiceplatform.repository.InventoryAlertRepository;
import com.pettrade.practiceplatform.repository.InventoryItemRepository;
import com.pettrade.practiceplatform.repository.InventoryLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryItemRepository itemRepository;
    @Mock
    private InventoryLogRepository logRepository;
    @Mock
    private InventoryAlertRepository alertRepository;

    private InventoryService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-03-25T12:00:00Z"), ZoneOffset.UTC);
        service = new InventoryService(itemRepository, logRepository, alertRepository, clock, 10);
    }

    @Test
    void stockEqualThresholdCreatesSingleOpenAlertAndNoSpam() {
        User merchant = user(11L, "merchant");

        InventoryItem newItem = new InventoryItem();
        newItem.setMerchantUser(merchant);
        newItem.setSku("DOG-001");
        newItem.setName("Dog Food");
        newItem.setStockQuantity(10L);

        when(itemRepository.findByMerchantUserIdAndSku(11L, "DOG-001")).thenReturn(Optional.empty());
        when(itemRepository.save(any(InventoryItem.class))).thenAnswer(i -> {
            InventoryItem it = i.getArgument(0);
            if (it.getId() == null) {
                ReflectionTestUtils.setField(it, "id", 1L);
            }
            return it;
        });
        when(alertRepository.findTopByItemIdAndStatusOrderByCreatedAtDesc(1L, InventoryAlertStatus.OPEN))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existingOpenAlert(1L, 10L)));
        when(alertRepository.save(any(InventoryAlert.class))).thenAnswer(i -> i.getArgument(0));

        InventoryItem persistedItem = new InventoryItem();
        ReflectionTestUtils.setField(persistedItem, "id", 1L);
        persistedItem.setMerchantUser(merchant);
        persistedItem.setSku("DOG-001");
        persistedItem.setName("Dog Food");
        persistedItem.setStockQuantity(10L);
        persistedItem.setAlertThreshold(10L);
        when(itemRepository.findByIdAndMerchantUserId(1L, 11L)).thenReturn(Optional.of(persistedItem));

        service.upsertItem(new InventoryItemUpsertRequest("DOG-001", "Dog Food", 10L, 10L), merchant);
        service.adjustStock(1L, new InventoryStockAdjustRequest(9L, "sale"), merchant);

        ArgumentCaptor<InventoryAlert> captor = ArgumentCaptor.forClass(InventoryAlert.class);
        verify(alertRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        List<InventoryAlert> savedAlerts = captor.getAllValues();
        assertEquals(2, savedAlerts.size());
        assertEquals(InventoryAlertStatus.OPEN, savedAlerts.get(0).getStatus());
        assertEquals(InventoryAlertStatus.OPEN, savedAlerts.get(1).getStatus());
    }

    @Test
    void risingAboveThresholdResolvesOpenAlert() {
        User merchant = user(12L, "merchant2");
        InventoryItem item = new InventoryItem();
        ReflectionTestUtils.setField(item, "id", 5L);
        item.setMerchantUser(merchant);
        item.setSku("CAT-010");
        item.setName("Cat Snacks");
        item.setStockQuantity(6L);
        item.setAlertThreshold(10L);

        InventoryAlert open = existingOpenAlert(5L, 6L);

        when(itemRepository.findByIdAndMerchantUserId(5L, 12L)).thenReturn(Optional.of(item));
        when(itemRepository.save(any(InventoryItem.class))).thenAnswer(i -> i.getArgument(0));
        when(alertRepository.findTopByItemIdAndStatusOrderByCreatedAtDesc(5L, InventoryAlertStatus.OPEN))
                .thenReturn(Optional.of(open));
        when(alertRepository.save(any(InventoryAlert.class))).thenAnswer(i -> i.getArgument(0));

        service.adjustStock(5L, new InventoryStockAdjustRequest(20L, "restock"), merchant);

        assertEquals(InventoryAlertStatus.RESOLVED, open.getStatus());
        assertNotNull(open.getResolvedAt());
    }

    @Test
    void nullThresholdUsesDefaultTen() {
        User merchant = user(13L, "merchant3");
        InventoryItem item = new InventoryItem();
        ReflectionTestUtils.setField(item, "id", 7L);
        item.setMerchantUser(merchant);
        item.setSku("BIRD-5");
        item.setName("Bird Seeds");
        item.setStockQuantity(15L);
        item.setAlertThreshold(null);

        when(itemRepository.findByIdAndMerchantUserId(7L, 13L)).thenReturn(Optional.of(item));
        when(itemRepository.save(any(InventoryItem.class))).thenAnswer(i -> i.getArgument(0));
        when(alertRepository.findTopByItemIdAndStatusOrderByCreatedAtDesc(7L, InventoryAlertStatus.OPEN))
                .thenReturn(Optional.empty());

        service.adjustStock(7L, new InventoryStockAdjustRequest(10L, "drop"), merchant);

        ArgumentCaptor<InventoryAlert> captor = ArgumentCaptor.forClass(InventoryAlert.class);
        verify(alertRepository).save(captor.capture());
        assertEquals(10L, captor.getValue().getThresholdValue());
        assertNull(captor.getValue().getResolvedAt());
    }

    private InventoryAlert existingOpenAlert(Long itemId, Long stock) {
        InventoryItem item = new InventoryItem();
        ReflectionTestUtils.setField(item, "id", itemId);
        item.setSku("SKU-" + itemId);
        InventoryAlert alert = new InventoryAlert();
        alert.setItem(item);
        alert.setThresholdValue(10L);
        alert.setStockQuantity(stock);
        alert.setStatus(InventoryAlertStatus.OPEN);
        alert.setMessage("open");
        ReflectionTestUtils.setField(alert, "createdAt", LocalDateTime.of(2026, 3, 25, 10, 0, 0));
        return alert;
    }

    private User user(Long id, String username) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", id);
        user.setUsername(username);
        return user;
    }
}
