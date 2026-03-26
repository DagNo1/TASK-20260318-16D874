package com.pettrade.practiceplatform.repository;

import com.pettrade.practiceplatform.domain.AttributeSpec;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttributeSpecRepository extends JpaRepository<AttributeSpec, Long> {
    List<AttributeSpec> findByIdIn(List<Long> ids);
}
