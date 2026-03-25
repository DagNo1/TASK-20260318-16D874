package com.pettrade.practiceplatform.repository;

import com.pettrade.practiceplatform.domain.InventoryLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface InventoryLogRepository extends JpaRepository<InventoryLog, Long> {
    List<InventoryLog> findByItemMerchantUserIdOrderByIdDesc(Long merchantUserId);
    List<InventoryLog> findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(LocalDateTime start, LocalDateTime end);
}
