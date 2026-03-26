package com.pettrade.practiceplatform.repository;

import com.pettrade.practiceplatform.domain.Brand;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandRepository extends JpaRepository<Brand, Long> {
}
