package com.pettrade.practiceplatform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pettrade.practiceplatform.api.notification.NotificationDeliveryView;
import com.pettrade.practiceplatform.domain.NotificationDelivery;
import com.pettrade.practiceplatform.domain.NotificationEvent;
import com.pettrade.practiceplatform.domain.NotificationSubscription;
import com.pettrade.practiceplatform.domain.User;
import com.pettrade.practiceplatform.domain.enumtype.NotificationEventType;
import com.pettrade.practiceplatform.repository.NotificationDeliveryRepository;
import com.pettrade.practiceplatform.repository.NotificationEventRepository;
import com.pettrade.practiceplatform.repository.NotificationSubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceIntegrationTest {

    @Mock
    private NotificationEventRepository eventRepository;
    @Mock
    private NotificationSubscriptionRepository subscriptionRepository;
    @Mock
    private NotificationDeliveryRepository deliveryRepository;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-03-25T12:00:00Z"), ZoneOffset.UTC);
        service = new NotificationService(
                eventRepository,
                subscriptionRepository,
                deliveryRepository,
                new ObjectMapper(),
                clock
        );
    }

    @Test
    void publishCreatesDeliveryWithDeliveredTimestampAndReadInitiallyNull() {
        User actor = user(10L, "actor");
        User recipient = user(20L, "receiver");

        NotificationSubscription sub = new NotificationSubscription();
        sub.setUser(recipient);
        sub.setEventType(NotificationEventType.PRACTICE_STEP_COMPLETED);
        sub.setEnabled(true);

        NotificationEvent event = new NotificationEvent();
        ReflectionTestUtils.setField(event, "id", 1L);
        event.setEventType(NotificationEventType.PRACTICE_STEP_COMPLETED);
        event.setPayloadJson("{\"stepId\":99}");
        event.setActorUser(actor);

        when(eventRepository.save(any(NotificationEvent.class))).thenReturn(event);
        when(subscriptionRepository.findByEventTypeAndEnabledTrue(NotificationEventType.PRACTICE_STEP_COMPLETED))
                .thenReturn(List.of(sub));

        service.publish(NotificationEventType.PRACTICE_STEP_COMPLETED, Map.of("stepId", 99), actor);
        verify(deliveryRepository).saveAll(org.mockito.ArgumentMatchers.<NotificationDelivery>anyList());

        when(deliveryRepository.findByRecipientIdOrderByDeliveredAtDesc(20L)).thenReturn(List.of(delivery(event, recipient, null)));
        List<NotificationDeliveryView> list = service.listForUser(recipient, false);
        assertEquals(1, list.size());
        assertNotNull(list.get(0).deliveredAt());
        assertNull(list.get(0).readAt());
    }

    @Test
    void markReadSetsReadTimestamp() {
        User recipient = user(20L, "receiver");
        NotificationEvent event = new NotificationEvent();
        ReflectionTestUtils.setField(event, "id", 2L);
        event.setEventType(NotificationEventType.IM_MESSAGE_RECALLED);
        event.setPayloadJson("{}");

        NotificationDelivery delivery = delivery(event, recipient, null);
        ReflectionTestUtils.setField(delivery, "id", 100L);

        when(deliveryRepository.findByIdAndRecipientId(100L, 20L)).thenReturn(Optional.of(delivery));
        when(deliveryRepository.save(any(NotificationDelivery.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationDeliveryView view = service.markRead(100L, recipient);
        assertNotNull(view.readAt());
    }

    @Test
    void publishOrderStatusUpdateCreatesDeliveryWithTimestamps() {
        User actor = user(30L, "order-actor");
        User recipient = user(40L, "order-recipient");

        NotificationEvent event = new NotificationEvent();
        ReflectionTestUtils.setField(event, "id", 3L);
        event.setEventType(NotificationEventType.ORDER_STATUS_UPDATED);
        event.setPayloadJson("{\"orderId\":123,\"status\":\"SHIPPED\"}");
        event.setActorUser(actor);

        when(eventRepository.save(any(NotificationEvent.class))).thenReturn(event);

        service.publishOrderStatusUpdate(123L, "SHIPPED", actor, List.of(recipient));
        verify(deliveryRepository).saveAll(org.mockito.ArgumentMatchers.<NotificationDelivery>anyList());

        when(deliveryRepository.findByRecipientIdAndReadAtIsNullOrderByDeliveredAtDesc(40L))
                .thenReturn(List.of(delivery(event, recipient, null)));

        NotificationDeliveryView unread = service.listForUser(recipient, true).get(0);
        assertEquals("ORDER_STATUS_UPDATED", unread.eventType());
        assertNotNull(unread.deliveredAt());
        assertNull(unread.readAt());
    }

    @Test
    void listAllSubscriptionsIncludesNewEventTypesDisabledByDefault() {
        User user = user(50L, "sub-user");

        when(subscriptionRepository.findByUserIdAndEventType(eq(50L), any(NotificationEventType.class)))
                .thenReturn(Optional.empty());

        List<String> eventTypes = service.listAllSubscriptions(user).stream()
                .map(v -> v.eventType())
                .toList();

        assertEquals(true, eventTypes.contains("REVIEW_RESULT_PUBLISHED"));
        assertEquals(true, eventTypes.contains("REPORT_HANDLING_UPDATED"));
        assertEquals(true, eventTypes.contains("ORDER_STATUS_UPDATED"));
    }

    private NotificationDelivery delivery(NotificationEvent event, User recipient, LocalDateTime readAt) {
        NotificationDelivery d = new NotificationDelivery();
        d.setEvent(event);
        d.setRecipient(recipient);
        d.setDeliveredAt(LocalDateTime.of(2026, 3, 25, 12, 0, 0));
        d.setReadAt(readAt);
        return d;
    }

    private User user(Long id, String username) {
        User u = new User();
        ReflectionTestUtils.setField(u, "id", id);
        u.setUsername(username);
        return u;
    }
}
