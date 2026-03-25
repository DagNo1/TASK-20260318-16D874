package com.pettrade.practiceplatform.repository;

import com.pettrade.practiceplatform.domain.NotificationDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, Long> {
    List<NotificationDelivery> findByRecipientIdOrderByDeliveredAtDesc(Long recipientId);
    List<NotificationDelivery> findByRecipientIdAndReadAtIsNullOrderByDeliveredAtDesc(Long recipientId);
    Optional<NotificationDelivery> findByIdAndRecipientId(Long id, Long recipientId);
}
