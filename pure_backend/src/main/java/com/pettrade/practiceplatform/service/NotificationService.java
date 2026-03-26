package com.pettrade.practiceplatform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pettrade.practiceplatform.api.notification.NotificationDeliveryView;
import com.pettrade.practiceplatform.api.notification.NotificationSubscriptionView;
import com.pettrade.practiceplatform.domain.NotificationDelivery;
import com.pettrade.practiceplatform.domain.NotificationEvent;
import com.pettrade.practiceplatform.domain.NotificationSubscription;
import com.pettrade.practiceplatform.domain.User;
import com.pettrade.practiceplatform.domain.enumtype.NotificationEventType;
import com.pettrade.practiceplatform.exception.NotFoundException;
import com.pettrade.practiceplatform.repository.NotificationDeliveryRepository;
import com.pettrade.practiceplatform.repository.NotificationEventRepository;
import com.pettrade.practiceplatform.repository.NotificationSubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    private final NotificationEventRepository eventRepository;
    private final NotificationSubscriptionRepository subscriptionRepository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public NotificationService(
            NotificationEventRepository eventRepository,
            NotificationSubscriptionRepository subscriptionRepository,
            NotificationDeliveryRepository deliveryRepository,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.eventRepository = eventRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.deliveryRepository = deliveryRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public void publish(NotificationEventType type, Map<String, Object> payload, User actor) {
        NotificationEvent event = new NotificationEvent();
        event.setEventType(type);
        event.setPayloadJson(toJson(payload));
        event.setActorUser(actor);
        NotificationEvent savedEvent = eventRepository.save(event);

        LocalDateTime now = LocalDateTime.now(clock);
        List<NotificationSubscription> subscriptions = subscriptionRepository.findByEventTypeAndEnabledTrue(type);
        List<NotificationDelivery> deliveries = subscriptions.stream().map(sub -> {
            NotificationDelivery d = new NotificationDelivery();
            d.setEvent(savedEvent);
            d.setRecipient(sub.getUser());
            d.setDeliveredAt(now);
            return d;
        }).toList();
        deliveryRepository.saveAll(deliveries);
    }

    @Transactional(readOnly = true)
    public List<NotificationDeliveryView> listForUser(User user, boolean unreadOnly) {
        List<NotificationDelivery> deliveries = unreadOnly
                ? deliveryRepository.findByRecipientIdAndReadAtIsNullOrderByDeliveredAtDesc(user.getId())
                : deliveryRepository.findByRecipientIdOrderByDeliveredAtDesc(user.getId());
        return deliveries
                .stream()
                .map(this::toView)
                .toList();
    }

    @Transactional
    public NotificationDeliveryView markRead(Long deliveryId, User user) {
        NotificationDelivery delivery = deliveryRepository.findByIdAndRecipientId(deliveryId, user.getId())
                .orElseThrow(() -> new NotFoundException("Notification delivery not found"));
        if (delivery.getReadAt() == null) {
            delivery.setReadAt(LocalDateTime.now(clock));
            delivery = deliveryRepository.save(delivery);
        }
        return toView(delivery);
    }

    @Transactional
    public NotificationSubscriptionView upsertSubscription(User user, NotificationEventType eventType, boolean enabled) {
        NotificationSubscription sub = subscriptionRepository.findByUserIdAndEventType(user.getId(), eventType)
                .orElseGet(() -> {
                    NotificationSubscription s = new NotificationSubscription();
                    s.setUser(user);
                    s.setEventType(eventType);
                    return s;
                });
        sub.setEnabled(enabled);
        NotificationSubscription saved = subscriptionRepository.save(sub);
        return new NotificationSubscriptionView(saved.getEventType().name(), saved.isEnabled());
    }

    @Transactional(readOnly = true)
    public List<NotificationSubscriptionView> listSubscriptions(User user) {
        return subscriptionRepository.findByUserIdAndEnabledTrue(user.getId()).stream()
                .map(s -> new NotificationSubscriptionView(s.getEventType().name(), s.isEnabled()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationSubscriptionView> listAllSubscriptions(User user) {
        return java.util.Arrays.stream(NotificationEventType.values())
                .map(type -> subscriptionRepository.findByUserIdAndEventType(user.getId(), type)
                        .map(s -> new NotificationSubscriptionView(s.getEventType().name(), s.isEnabled()))
                        .orElseGet(() -> new NotificationSubscriptionView(type.name(), false)))
                .toList();
    }

    @Transactional
    public void publishOrderStatusUpdate(Long orderId, String status, User actor, List<User> recipients) {
        // TODO: Replace with dedicated order module event publisher when order domain is introduced.
        publishToRecipients(
                NotificationEventType.ORDER_STATUS_UPDATED,
                Map.of("orderId", orderId, "status", status),
                actor,
                recipients
        );
    }

    @Transactional
    public void publishToRecipients(NotificationEventType type, Map<String, Object> payload, User actor, List<User> recipients) {
        NotificationEvent event = new NotificationEvent();
        event.setEventType(type);
        event.setPayloadJson(toJson(payload));
        event.setActorUser(actor);
        NotificationEvent savedEvent = eventRepository.save(event);

        LocalDateTime now = LocalDateTime.now(clock);
        List<NotificationDelivery> deliveries = new ArrayList<>();
        for (User recipient : recipients) {
            NotificationDelivery d = new NotificationDelivery();
            d.setEvent(savedEvent);
            d.setRecipient(recipient);
            d.setDeliveredAt(now);
            deliveries.add(d);
        }
        deliveryRepository.saveAll(deliveries);
    }

    private NotificationDeliveryView toView(NotificationDelivery delivery) {
        Long actorUserId = delivery.getEvent().getActorUser() == null ? null : delivery.getEvent().getActorUser().getId();
        return new NotificationDeliveryView(
                delivery.getId(),
                delivery.getEvent().getEventType().name(),
                delivery.getEvent().getPayloadJson(),
                actorUserId,
                delivery.getDeliveredAt(),
                delivery.getReadAt()
        );
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize notification payload", e);
        }
    }
}
