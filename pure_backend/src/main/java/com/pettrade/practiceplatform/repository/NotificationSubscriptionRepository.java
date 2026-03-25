package com.pettrade.practiceplatform.repository;

import com.pettrade.practiceplatform.domain.NotificationSubscription;
import com.pettrade.practiceplatform.domain.enumtype.NotificationEventType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationSubscriptionRepository extends JpaRepository<NotificationSubscription, Long> {
    List<NotificationSubscription> findByUserIdAndEnabledTrue(Long userId);
    List<NotificationSubscription> findByEventTypeAndEnabledTrue(NotificationEventType eventType);
    Optional<NotificationSubscription> findByUserIdAndEventType(Long userId, NotificationEventType eventType);
}
