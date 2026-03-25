package com.pettrade.practiceplatform.repository;

import com.pettrade.practiceplatform.domain.NotificationEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationEventRepository extends JpaRepository<NotificationEvent, Long> {
}
