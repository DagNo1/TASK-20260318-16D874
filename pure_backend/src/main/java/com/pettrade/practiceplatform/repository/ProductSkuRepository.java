package com.pettrade.practiceplatform.repository;

import com.pettrade.practiceplatform.domain.ProductSku;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductSkuRepository extends JpaRepository<ProductSku, Long> {
    List<ProductSku> findByProductIdOrderByIdAsc(Long productId);
}
