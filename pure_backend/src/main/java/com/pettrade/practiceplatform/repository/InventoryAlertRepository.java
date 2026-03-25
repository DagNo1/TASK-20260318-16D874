package com.pettrade.practiceplatform.repository;

import com.pettrade.practiceplatform.domain.InventoryAlert;
import com.pettrade.practiceplatform.domain.enumtype.InventoryAlertStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InventoryAlertRepository extends JpaRepository<InventoryAlert, Long> {
    Optional<InventoryAlert> findTopByItemIdAndStatusOrderByCreatedAtDesc(Long itemId, InventoryAlertStatus status);
    List<InventoryAlert> findByItemMerchantUserIdOrderByCreatedAtDesc(Long merchantUserId);
    List<InventoryAlert> findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(LocalDateTime start, LocalDateTime end);
}
