package com.pettrade.practiceplatform.repository;

import com.pettrade.practiceplatform.domain.UserSensitiveProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSensitiveProfileRepository extends JpaRepository<UserSensitiveProfile, Long> {
}
