package com.pettrade.practiceplatform.repository;

import com.pettrade.practiceplatform.domain.ImImageAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ImImageAssetRepository extends JpaRepository<ImImageAsset, Long> {
    Optional<ImImageAsset> findByFingerprint(String fingerprint);
}
