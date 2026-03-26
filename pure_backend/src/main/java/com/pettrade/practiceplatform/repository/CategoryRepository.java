package com.pettrade.practiceplatform.repository;

import com.pettrade.practiceplatform.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}
