package com.pettrade.practiceplatform.repository;

import com.pettrade.practiceplatform.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByIdAndMerchantUserId(Long id, Long merchantUserId);
    List<Product> findByMerchantUserIdAndListedOrderByIdDesc(Long merchantUserId, boolean listed);
}
