package com.pettrade.practiceplatform.repository;

import com.pettrade.practiceplatform.domain.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {
    Optional<InventoryItem> findByIdAndMerchantUserId(Long id, Long merchantUserId);
    Optional<InventoryItem> findByMerchantUserIdAndSku(Long merchantUserId, String sku);
    List<InventoryItem> findByMerchantUserId(Long merchantUserId);
}
